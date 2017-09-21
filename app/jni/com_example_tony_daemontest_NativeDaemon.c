#include <stdio.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <time.h>
#include <sys/inotify.h>
#include <stdlib.h>
#include <unistd.h>
#include <jni.h>
#include <android/log.h>

#define TAG		"NativeDaemon"

#define LOGI(...)	__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...)	__android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...)	__android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define	LOGE(...)	__android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define	DAEMON_CALLBACK_NAME		"onDaemonDead"

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
 * call java callback
 */
void java_callback(JNIEnv* env, jobject jobj, char* method_name){
	jclass cls = (*env)->GetObjectClass(env, jobj);
	jmethodID cb_method = (*env)->GetMethodID(env, cls, method_name, "()V");
	(*env)->CallVoidMethod(env, jobj, cb_method);
}

/**
 * start a android service
 */
void start_service(char* package_name, char* service_name, int sdkVersion) {
	pid_t pid = fork();
	if (pid < 0) {
		//error, do nothing...
	} else if (pid == 0) {
		if (package_name == NULL || service_name == NULL) {
			exit(EXIT_SUCCESS);
		}
		char* pkg_svc_name = str_stitching(package_name, "/", service_name);
		if (sdkVersion >= 17 || sdkVersion == 0) {
			execlp("am", "am", "startservice", "--user", "0", "-n", pkg_svc_name, (char *) NULL);
		} else {
			execlp("am", "am", "startservice", "-n", pkg_svc_name, (char *) NULL);
		}
		exit(EXIT_SUCCESS);
	} else {
		waitpid(pid, NULL, 0);
	}
}

int lock_file(char* lock_file_path) {
	int lockFileDescriptor = open(lock_file_path, O_RDONLY);
	if (lockFileDescriptor == -1) {
		lockFileDescriptor = open(lock_file_path, O_CREAT, S_IRUSR);
	}
	int lockRet = flock(lockFileDescriptor, LOCK_EX);
	if (lockRet == -1) {
		LOGE("pid:%d lock file failed >> %s <<", getpid(), lock_file_path);
		return 0;
	} else {
		LOGE("pid:%d lock file success  >> %s <<", getpid(), lock_file_path);
		return 1;
	}
}

/**
 * a2 b2
 */
notify_and_waitfor(char *observer_self_path, char *observer_daemon_path) {
	int observer_self_descriptor = open(observer_self_path, O_RDONLY);
	if (observer_self_descriptor == -1) {
		observer_self_descriptor = open(observer_self_path, O_CREAT,
		S_IRUSR | S_IWUSR);
	}
	while (open(observer_daemon_path, O_RDONLY) == -1) {
		usleep(1000);
	}
	int observer_daemon_descriptor = open(observer_daemon_path, O_RDONLY);
	remove(observer_daemon_path);
}

JNIEXPORT void JNICALL Java_com_example_tony_daemontest_NativeDaemon_doDaemon(JNIEnv *env, jobject jobj, jstring indicatorSelfPath, jstring indicatorDaemonPath, jstring observerSelfPath, jstring observerDaemonPath, jstring pkgName, jstring svcName, jint sdkVersion) {
	if(indicatorSelfPath == NULL || indicatorDaemonPath == NULL || observerSelfPath == NULL || observerDaemonPath == NULL || pkgName == NULL || svcName == NULL || sdkVersion == NULL) {
		LOGE("parameters cannot be NULL !");
		return;
	}

    char* a1 = (char*)(*env)->GetStringUTFChars(env, indicatorSelfPath, 0);
	char* b1 = (char*)(*env)->GetStringUTFChars(env, indicatorDaemonPath, 0);
	char* a2 = (char*)(*env)->GetStringUTFChars(env, observerSelfPath, 0);
	char* b2 = (char*)(*env)->GetStringUTFChars(env, observerDaemonPath, 0);
    char* package_name = (char*)(*env)->GetStringUTFChars(env, pkgName, 0);
    char* service_name = (char*)(*env)->GetStringUTFChars(env, svcName, 0);
    int sdk_version = sdkVersion;

	int cpid = getpid();
	char* indicator_self_path = a1;
	char* indicator_daemon_path = b1;
	char* observer_self_path = a2;
	char* observer_daemon_path = b2;
	LOGE("main pid:%d", getpid());
//	__pid_t pid = fork();
//	switch (pid) {
//		case -1:
//		perror("main fork failed");
//		exit(1);
//		break;
//		case 0:
//		setsid();
//		pid = fork();
//		if (pid > 0) {
//			exit(0);
//			break;
//		} else if (pid < 0) {
//			perror("child fork failed");
//			exit(1);
//			break;
//		}
//		chdir("/");
//		umask(0);
//		int i;
//		for (i = 0; i < 3; i++) {
//			close(i);
//		}
//		/*这时创建完守护进程，以下开始正式进入守护进程工作*/
//		indicator_self_path = b1;
//		indicator_daemon_path = a1;
//		observer_self_path = b2;
//		observer_daemon_path = a2;
//		break;
//		default:
//		LOGE("main ppid:%d pid:%d", getppid(), getpid());
//		break;
//	}
	LOGE("process pid:%d", getpid());
	int lock_status = 0;
	int try_time = 0;
	while (try_time < 3 && !(lock_status = lock_file(indicator_self_path))) {
		try_time++;
		LOGE("Persistent lock myself failed and try again as %d times", try_time);
	}
	//锁自己结果
	if (!lock_status) {
		LOGE("Persistent lock myself failed and exit");
		return;
	}

	notify_and_waitfor(observer_self_path, observer_daemon_path);
	LOGE("process:%d start lock other process's file  >> %s <<", getpid(), indicator_daemon_path);
	lock_status = lock_file(indicator_daemon_path);
	if (lock_status) {
	    LOGE("other process is killed");
        remove(observer_self_path);// it`s important ! to prevent from deadlock
//		if (cpid == getpid()) {
//			//主进程
//			java_callback(env, jobj, DAEMON_CALLBACK_NAME);
//		} else {
			//native进程
            char* pkg_svc_name = str_stitching(package_name, "/", service_name);
            if (sdkVersion >= 17 || sdkVersion == 0) {
                execlp("am", "am", "startservice", "--user", "0", "-n", pkg_svc_name, (char *) NULL);
            } else {
                execlp("am", "am", "startservice", "-n", pkg_svc_name, (char *) NULL);
            }
//		}
	}
}

