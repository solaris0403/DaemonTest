#include <fcntl.h>
#include <sys/stat.h>
#include <sys/inotify.h>
#include <stdlib.h>
#include <stdio.h>

#include "com_example_tony_daemontest_NativeDaemon.h"
#include "constant.h"
#include "common.c"
#include <errno.h>
#include <jni.h>

JNIEXPORT void JNICALL Java_com_example_tony_daemontest_NativeDaemon_doDaemon(JNIEnv *env, jobject jobj,
		jstring selfDaemonPath, jstring selfDaemonName,
		jstring selfServiceFile, jstring otherServiceFile,
		jstring selfServiceFileTmp, jstring otherServiceFileTmp,
		jstring selfNativeFile, jstring otherNativeFile,
		jstring selfNativeFileTmp, jstring otherNativeFileTmp,
		jstring selfPkgName, jstring otherPkgName,
		jstring selfSvcName, jstring otherSvcName,
		jint sdkVersion, jboolean isCoreService) {

//	if(selfDaemonPath == NULL || selfDaemonName == NULL
//			|| selfServiceFile == NULL || otherServiceFile == NULL
//			|| selfServiceFileTmp == NULL || otherServiceFileTmp == NULL
//			|| selfNativeFile == NULL || otherNativeFile == NULL
//			|| selfNativeFileTmp == NULL || otherNativeFileTmp == NULL
//			|| selfPkgName == NULL || otherPkgName == NULL
//			|| selfSvcName == NULL || otherSvcName == NULL
//			|| sdkVersion == NULL || isCoreService == NULL) {
//		LOGE("parameters cannot be NULL !!!");
//		return;
//	}
	//二进制可执行文件
	char* self_daemon_path = (char*)(*env)->GetStringUTFChars(env, selfDaemonPath, 0);
	char* self_daemon_name = (char*)(*env)->GetStringUTFChars(env, selfDaemonName, 0);

	//service需要的文件
	char* self_service_file = (char*)(*env)->GetStringUTFChars(env, selfServiceFile, 0);
	char* self_service_file_tmp = (char*)(*env)->GetStringUTFChars(env, selfServiceFileTmp, 0);
	char* other_native_file = (char*)(*env)->GetStringUTFChars(env, otherNativeFile, 0);
	char* other_native_file_tmp = (char*)(*env)->GetStringUTFChars(env, otherNativeFileTmp, 0);

	//native需要的文件
	char* self_native_file = (char*)(*env)->GetStringUTFChars(env, selfNativeFile, 0);
	char* self_native_file_tmp = (char*)(*env)->GetStringUTFChars(env, selfNativeFileTmp, 0);
	char* other_service_file = (char*)(*env)->GetStringUTFChars(env, otherServiceFile, 0);
	char* other_service_file_tmp = (char*)(*env)->GetStringUTFChars(env, otherServiceFileTmp, 0);
	//包名/服务名
	char* self_pkg_name = (char*)(*env)->GetStringUTFChars(env, selfPkgName, 0);
	char* self_svc_name = (char*)(*env)->GetStringUTFChars(env, selfSvcName, 0);
	char* other_pkg_name = (char*)(*env)->GetStringUTFChars(env, otherPkgName, 0);
	char* other_svc_name = (char*)(*env)->GetStringUTFChars(env, otherSvcName, 0);
	//sdk版本
	int sdk_version = sdkVersion;
	//是否为优先守护进程
	int is_core_service = isCoreService;

    LOGE("selfDaemonPath %s",self_daemon_path);
    LOGE("selfDaemonName %s",self_daemon_name);
    LOGE("selfServiceFile %s",self_service_file);
    LOGE("otherServiceFile %s",other_service_file);
    LOGE("selfServiceFileTmp %s",self_service_file_tmp);
    LOGE("otherServiceFileTmp %s",other_service_file_tmp);
    LOGE("selfNativeFile %s",self_native_file);
    LOGE("otherNativeFile %s",other_native_file);
    LOGE("selfNativeFileTmp %s",self_native_file_tmp);
    LOGE("otherNativeFileTmp %s",other_native_file_tmp);
    LOGE("selfPkgName %s",self_pkg_name);
    LOGE("otherPkgName %s",other_pkg_name);
    LOGE("selfSvcName %s",self_svc_name);
    LOGE("otherSvcName %s",other_svc_name);
    LOGE("sdkVersion %d",sdk_version);
    LOGE("sCoreService %d",is_core_service);

	//杀掉上一次fork的进程
	kill_zombie_process(self_daemon_name);

	//启动native进程
	int pid = fork();
	if(pid == 0) {
		execlp(self_daemon_path,
				self_daemon_name,
				PARAM_OTHER_PKG_NAME, other_pkg_name,
				PARAM_OTHER_SVC_NAME, other_svc_name,
				PARAM_OTHER_SERVICE_FILE, other_service_file,
				PARAM_OTHER_SERVICE_FILE_TMP, other_service_file_tmp,
				PARAM_SLEF_NATIVE_FILE, self_native_file,
				PARAM_SLEF_NATIVE_FILE_TMP, self_native_file_tmp,
				PARAM_IS_CORE_SERVICE, is_core_service,
				(char*) NULL);
		LOGE("启动文件进程失败：%d", errno);
	} else if(pid > 0) {
		int lock_status = 0;
		int try_time = 0;
		while(try_time < 3 && !(lock_status = lock_file(self_service_file))) {
			try_time++;
			LOGD("Persistent lock myself failed and try again as %d times", try_time);
			usleep(10000);
		}
		if(!lock_status) {
			LOGE("Persistent lock myself failed and exit");
			return;
		}
		LOGE("%s-自锁完成", self_service_file);
		LOGE("%d-self_service_file", getpid());
		notify_and_waitfor(self_service_file_tmp, other_native_file_tmp);
		lock_status = lock_file(other_native_file);
		if(lock_status) {
            flock(self_service_file, LOCK_UN);
            flock(other_native_file, LOCK_UN);
			LOGE("%s 进程fork出的native进程死亡.................", other_svc_name);
//            int count = 1;
//            while(count <= 50){
//                start_service("com.example.tony.daemontest", "com.example.tony.daemontest.SuperService");
//                LOGE("service进程循环执行：第%d次", count);
//                count++;
//            }
			java_callback(env, jobj, DAEMON_CALLBACK_NAME);
		}
	}
}
