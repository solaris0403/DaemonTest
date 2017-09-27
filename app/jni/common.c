#include <stdlib.h>
#include <stdio.h>
#include <sys/inotify.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <dirent.h>
#include <signal.h>


#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <syslog.h>
#include <sys/param.h>
#include <dirent.h>

#include "log.h"

/**
 *  get the android version code
 */
int get_version() {
	char value[8] = "";
	__system_property_get("ro.build.version.sdk", value);
	return atoi(value);
}

/**
 *  stitch three string to one
 */
char *str_stitching(const char *str1, const char *str2, const char *str3) {
	char *result;
	result = (char*) malloc(strlen(str1) + strlen(str2) + strlen(str3) + 1);
	if (!result) {
		return NULL;
	}
	strcpy(result, str1);
	strcat(result, str2);
	strcat(result, str3);
	return result;
}

/**
 * get android context
 */
jobject get_context(JNIEnv* env, jobject jobj) {
	jclass thiz_cls = (*env)->GetObjectClass(env, jobj);
	jfieldID context_field = (*env)->GetFieldID(env, thiz_cls, "mContext", "Landroid/content/Context;");
	return (*env)->GetObjectField(env, jobj, context_field);
}

char* get_package_name(JNIEnv* env, jobject jobj) {
	jobject context_obj = get_context(env, jobj);
	jclass context_cls = (*env)->GetObjectClass(env, context_obj);
	jmethodID getpackagename_method = (*env)->GetMethodID(jobj, context_cls,
			"getPackageName", "()Ljava/lang/String;");
	jstring package_name = (jstring)(*env)->CallObjectMethod(env, context_obj,
			getpackagename_method);
	return (char*) (*env)->GetStringUTFChars(env, package_name, 0);
}

/**
 * call java callback
 */
void java_callback(JNIEnv* env, jobject jobj, char* method_name) {
	jclass cls = (*env)->GetObjectClass(env, jobj);
	jmethodID cb_method = (*env)->GetMethodID(env, cls, method_name, "()V");
	(*env)->CallVoidMethod(env, jobj, cb_method);
}

/**
 * start a android service
 */
void start_service(char* package_name, char* service_name) {
	pid_t pid = fork();
	if (pid < 0) {
		//error, do nothing...
	} else if (pid == 0) {
	    setsid();
	    int i;
	    for(i=0; i<3; i++){
	        close(i);
	    }
	    chdir("/");
	    umask(0);
		if (package_name == NULL || service_name == NULL) {
			exit(EXIT_SUCCESS);
		}
		int version = get_version();
		char* pkg_svc_name = str_stitching(package_name, "/", service_name);

		if (version >= 17 || version == 0) {
			execlp("am", "am", "startservice", "--user", "0", "-n", pkg_svc_name, (char *) NULL);
		} else {
			execlp("am", "am", "startservice", "-n", pkg_svc_name, (char *) NULL);
		}
		exit(EXIT_SUCCESS);
	} else {
	    exit(EXIT_SUCCESS);
//		waitpid(pid, NULL, 0);
	}
}

/**
 * start a android service
 */
void send_broadcast(char* action) {
	pid_t pid = fork();
	if (pid < 0) {
		//error, do nothing...
	} else if (pid == 0) {
		if (action == NULL) {
			exit(EXIT_SUCCESS);
		}
		LOGE("send broadcast");
		execlp("am", "am", "broadcast", "-a", action, (char *) NULL);
		exit(EXIT_SUCCESS);
	} else {
//		waitpid(pid, NULL, 0);
	}
}

/**
 * start a android service
 */
void stop_service(char* package_name, char* service_name) {
	pid_t pid = fork();
	if (pid < 0) {
		//error, do nothing...
	} else if (pid == 0) {
		if (package_name == NULL || service_name == NULL) {
			exit(EXIT_SUCCESS);
		}
		int version = get_version();
		char* pkg_svc_name = str_stitching(package_name, "/", service_name);
		if (version >= 17 || version == 0) {
			execlp("am", "am", "force-stop", "--user", "0", "-n",
					pkg_svc_name, (char *) NULL);
		} else {
			execlp("am", "am", "force-stop", "-n", pkg_svc_name,
					(char *) NULL);
		}
		exit(EXIT_SUCCESS);
	} else {
		waitpid(pid, NULL, 0);
	}
}

void notify_and_waitfor(char *observer_self_path, char *observer_daemon_path) {
	//操作目录权限
	int observer_self_descriptor = open(observer_self_path, O_RDONLY);
	if (observer_self_descriptor == -1) {
		//创建当前进程临时文件,用于其他进程监听
		observer_self_descriptor = open(observer_self_path, O_CREAT,S_IRUSR | S_IWUSR);
	}
	int observer_daemon_descriptor = open(observer_daemon_path, O_RDONLY);
	while (observer_daemon_descriptor == -1) {
		//循环打开other文件，如果没打开，说明没有创建好
		usleep(1000);
		observer_daemon_descriptor = open(observer_daemon_path, O_RDONLY);
	}
	//将对方的文件删除
	remove(observer_daemon_path);
	//自身文件加锁成功
	LOGE("%s 进程准备就绪", observer_daemon_path);
}

/**
 *  Lock the file, this is block method.
 */
int lock_file(char* lock_file_path) {
	//native权限问题
	LOGD("start try to lock file >> %s <<", lock_file_path);
	int lockFileDescriptor = open(lock_file_path, O_RDONLY);
	if (lockFileDescriptor == -1) {
		lockFileDescriptor = open(lock_file_path, O_CREAT, S_IRUSR);
	}
	int lockRet = flock(lockFileDescriptor, LOCK_EX);
	if (lockRet == -1) {
		LOGE("lock file failed >> %s <<", lock_file_path);
		return 0;
	} else {
		LOGD("lock file success  >> %s <<", lock_file_path);
		return 1;
	}
}

/**
 *  get the process pid by process name
 */
int find_pid_by_name(char *pid_name, int *pid_list) {
	DIR *dir;
	struct dirent *next;
	int i = 0;
	pid_list[0] = 0;
	dir = opendir("/proc");
	if (!dir) {
		return 0;
	}
	while ((next = readdir(dir)) != NULL) {
		FILE *status;
		char proc_file_name[BUFFER_SIZE];
		char buffer[BUFFER_SIZE];
		char process_name[BUFFER_SIZE];

		if (strcmp(next->d_name, "..") == 0) {
			continue;
		}
		if (!isdigit(*next->d_name)) {
			continue;
		}
		sprintf(proc_file_name, "/proc/%s/cmdline", next->d_name);
		if (!(status = fopen(proc_file_name, "r"))) {
			continue;
		}
		if (fgets(buffer, BUFFER_SIZE - 1, status) == NULL) {
			fclose(status);
			continue;
		}
		fclose(status);
		sscanf(buffer, "%[^-]", process_name);
		if (strcmp(process_name, pid_name) == 0) {
			pid_list[i++] = atoi(next->d_name);
		}
	}
	if (pid_list) {
		pid_list[i] = 0;
	}
	closedir(dir);
	return i;
}

/**
 *  kill all process by name
 */
void kill_zombie_process(char* zombie_name) {
	int pid_list[200];
	int total_num = find_pid_by_name(zombie_name, pid_list);
	LOGD("zombie process name is %s, and number is %d, killing...", zombie_name,
			total_num);
	int i;
	for (i = 0; i < total_num; i++) {
		int retval = 0;
		int daemon_pid = pid_list[i];
		if (daemon_pid > 1 && daemon_pid != getpid()
				&& daemon_pid != getppid()) {
			retval = kill(daemon_pid, SIGTERM);
			if (!retval) {
				LOGD("kill zombie successfully, zombie`s pid = %d", daemon_pid);
			} else {
				LOGE("kill zombie failed, zombie`s pid = %d", daemon_pid);
			}
		}
	}
}
