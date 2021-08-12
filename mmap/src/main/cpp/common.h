#ifndef _common_h
#define _common_h

#include <cerrno>
#include <cstdlib>
#include <cstring>
#include <jni.h>

static const size_t ERR_MSG_SZ = 128;
static const size_t ERRNO_MSG_SZ = 64;

inline void throw_err(JNIEnv *env, const char *msg) {
    jclass xklass = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(xklass, msg);
}

inline void throw_err_fmt(JNIEnv *env, const char *fmt, ...) {
    char buf[ERR_MSG_SZ];

    va_list args;
    va_start(args, fmt);
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);

    throw_err(env, buf);
}

inline void throw_errno_fmt(JNIEnv *env, const char *fmt, ...) {
    char buf[ERR_MSG_SZ];

    va_list args;
    va_start(args, fmt);
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);

    if (errno) {
        char errno_buf[ERRNO_MSG_SZ];
        snprintf(errno_buf, sizeof(errno_buf), ": errno=%d %s", errno, strerror(errno));
        strncat(buf, errno_buf, ERRNO_MSG_SZ);
    }

    throw_err(env, buf);
}

#endif
