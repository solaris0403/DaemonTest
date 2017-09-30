#include <stdlib.h>
#include <unistd.h>

#include "constant.h"
#include "common.c"

int main(int argc, char *argv[]){
    pid_t pid = fork();
    if(pid == 0){
        setsid();
        int f;
        for(f=0; f<3; f++){
            close(f);
        }
        chdir("/");
        umask(0);
        char* other_pkg_name;
        char* other_svc_name;
        char* other_service_file;
        char* other_service_file_tmp;
        char* self_native_file;
        char* self_native_file_tmp;
        int is_core_service;
        if(argc < 14){
            LOGE("daemon parameters error");
            return 0;
        }
        int i;
        for (i = 0; i < argc; i ++){
            if(argv[i] == NULL){
                continue;
            }
            if (!strcmp(PARAM_OTHER_PKG_NAME, argv[i])){
                other_pkg_name = argv[i + 1];
            }else if (!strcmp(PARAM_OTHER_SVC_NAME, argv[i]))	{
                other_svc_name = argv[i + 1];
            }else if (!strcmp(PARAM_OTHER_SERVICE_FILE, argv[i])){
                other_service_file = argv[i + 1];
            }else if (!strcmp(PARAM_OTHER_SERVICE_FILE_TMP, argv[i]))	{
                other_service_file_tmp = argv[i + 1];
            }else if (!strcmp(PARAM_SLEF_NATIVE_FILE, argv[i]))	{
                self_native_file = argv[i + 1];
            }else if (!strcmp(PARAM_SLEF_NATIVE_FILE_TMP, argv[i]))	{
                self_native_file_tmp = argv[i + 1];
            }else if (!strcmp(PARAM_IS_CORE_SERVICE, argv[i]))	{
                is_core_service = atoi(argv[i + 1]);
            }
        }
        int lock_status = 0;
        int try_time = 0;
        while(try_time < 3 && !(lock_status = lock_file(self_native_file))){
            try_time++;
            LOGD("Persistent lock myself failed and try again as %d times", try_time);
            usleep(1000);
        }
        if(!lock_status){
            LOGE("Persistent lock myself failed and exit");
            return 0;
        }
        LOGE("%s-自锁完成", self_native_file);
        if(0==fork()){
            LOGE("发送广播");
//            execlp("am", "am", "broadcast", "--user", "0", "-a", "com.example.tony.daemontest.DaemonReceiver", (char *) NULL);
//            start_service("com.example.tony.daemontest", "com.example.tony.daemontest.SuperService");
        }
        notify_and_waitfor(self_native_file_tmp, other_service_file_tmp);
        lock_status = lock_file(other_service_file);
        if(lock_status){
            flock(self_native_file, LOCK_UN);
            flock(other_service_file, LOCK_UN);
            while(1){
                LOGE("%s 进程死亡.................", other_svc_name);
                usleep(1000);
            }
//            remove(other_service_file);
//            int count = 1;
//            while(count <= 200){
//                start_service("com.example.tony.daemontest", "com.example.tony.daemontest.SuperService");
//                LOGE("native进程循环执行：第%d次", count);
//                count++;
//            }
        }
    }else{
        exit(EXIT_SUCCESS);
    }
}
