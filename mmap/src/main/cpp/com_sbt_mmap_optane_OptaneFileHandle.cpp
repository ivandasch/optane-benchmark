#include <libpmem.h>
#include <unistd.h>
#include "common.h"
#include "com_sbt_mmap_optane_OptaneFileHandle.h"

static const char *CTX_CLS_NAME = "com/sbt/mmap/optane/OptaneFileHandler$Context";

inline void *mmap(JNIEnv* env, const char* path, const size_t sz, size_t *mapped_len, int *is_pmem) {
    errno = 0;
    void* addr = pmem_map_file(path, sz, PMEM_FILE_CREATE, 0666, mapped_len, is_pmem);

    if (addr == nullptr) {
        throw_errno_fmt(env, "Failed to map %s", path);
        return nullptr;
    }
    else {
        return addr;
    }
}

inline void unmap(JNIEnv* env, void *addr, size_t len) {
    errno = 0;
    if (pmem_unmap(addr, len) != 0) {
        throw_errno_fmt(env, "Failed to unmap buffer at %x", addr);
    }
}

JNIEXPORT jobject JNICALL Java_com_sbt_mmap_optane_OptaneFileHandler_mmap(
        JNIEnv *env,
        jclass,
        jstring path,
        jlong sz
) {
    const char *path_ = env->GetStringUTFChars(path, nullptr);
    size_t mapped_len;
    int is_pmem;

    void* pmemaddr = mmap(env, path_, (size_t)sz, &mapped_len, &is_pmem);
    env->ReleaseStringUTFChars(path, path_);

    if (pmemaddr == nullptr) {
        return nullptr;
    }

    jclass ctxClazz = env->FindClass(CTX_CLS_NAME);
    if (!ctxClazz) {
        throw_err_fmt(env, "Could not load %s", CTX_CLS_NAME);
        unmap(env, pmemaddr, mapped_len);
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(ctxClazz , "<init>", "(Ljava/nio/ByteBuffer;Z)V");
    if (!constructor) {
        throw_err_fmt(env, "Could not obtain %s constructor", CTX_CLS_NAME);
        unmap(env, pmemaddr, sz);
        return nullptr;
    }

    jobject bbuf = env->NewDirectByteBuffer(pmemaddr, (jlong)mapped_len);

    if (!bbuf) {
        throw_err_fmt(env, "Could not create java.nio.DirectByteBuffer from address %x with length %x",
                      pmemaddr, mapped_len);
        unmap(env, pmemaddr, mapped_len);
        return nullptr;
    }

    return env->NewObject(ctxClazz, constructor, bbuf , (jboolean)is_pmem);
}


JNIEXPORT void JNICALL Java_com_sbt_mmap_optane_OptaneFileHandler_munmap(
        JNIEnv *env,
        jclass,
        jobject buf
) {
    void *addr = env->GetDirectBufferAddress(buf);
    size_t len = env->GetDirectBufferCapacity(buf);
    unmap(env, addr, len);
}


JNIEXPORT void JNICALL Java_com_sbt_mmap_optane_OptaneFileHandler_force(
        JNIEnv *env,
        jclass,
        jobject buf,
        jlong offset,
        jlong length,
        jboolean isPmem
) {
    errno = 0;
    void *addr = env->GetDirectBufferAddress(buf);

    if (isPmem) {
        pmem_persist((void*)((size_t)addr + offset), length);
    }
    else {
        if (pmem_msync((void*)((size_t)addr + offset), length) != 0) {
            throw_errno_fmt(env, "Failed to msync buffer at %x with offset %x and length %x",
                            addr, offset, length);
        }
    }
}
