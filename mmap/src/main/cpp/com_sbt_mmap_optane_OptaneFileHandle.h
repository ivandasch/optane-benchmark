#ifndef _com_sbt_mmap_optane_OptaneFileHandler
#define _com_sbt_mmap_optane_OptaneFileHandler

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jobject JNICALL Java_com_sbt_mmap_optane_OptaneFileHandler_mmap(JNIEnv *, jclass, jstring, jlong);

JNIEXPORT void JNICALL Java_com_sbt_mmap_optane_OptaneFileHandler_munmap(JNIEnv *, jclass, jobject);

JNIEXPORT void JNICALL Java_com_sbt_mmap_optane_OptaneFileHandler_force(JNIEnv *, jclass, jobject, jlong, jlong, jboolean);

#ifdef __cplusplus
}
#endif
#endif
