#include <cutils/xlog.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/mman.h>

#define LOG_TAG "ext_alloc"
#define EXTALLOC_PRINTF XLOGE
#define UIO_DEV "/dev/uio0"

void* uspace_ext_malloc(size_t bytes) {
    void * vaddr = NULL;
    static int uio_fd = -1;

    if(uio_fd < 0){
        uio_fd  = open(UIO_DEV,O_RDWR);

        if (uio_fd <0){
            EXTALLOC_PRINTF("%s[%d] open device failed, uio_fd: %d, error: %s\n", __FUNCTION__, __LINE__, uio_fd, strerror(errno));
            return -1;
        }
    }
       
    vaddr = mmap(NULL, bytes, PROT_READ | PROT_WRITE, MAP_SHARED, uio_fd, 0); 
    if (vaddr == MAP_FAILED){
        EXTALLOC_PRINTF("%s[%d] mmap failed, error: %s\n", __FUNCTION__, __LINE__, strerror(errno));
        
        perror("mmap");
        close(uio_fd);
        exit(-1);
    }

    //EXTALLOC_PRINTF("%s, 0x%x(0x%x)\n", __FUNCTION__, vaddr, bytes);

    return vaddr; 
}

void* mtk_ext_malloc(size_t bytes) {
#ifdef MTK_USE_RESERVED_EXT_MEM	
    return uspace_ext_malloc(bytes);
#else
    return malloc(bytes);
#endif
}

void uspace_ext_free(void* mem) {

    if (mem != 0){
        //EXTALLOC_PRINTF("%s, 0x%x\n", __FUNCTION__, mem);

        /* give a default unmap page size, a correct size will be given in kernel. */
        munmap(mem, 4096);
    }else{
        EXTALLOC_PRINTF("%s, error! NULL ptr\n", __FUNCTION__);
    }
}

void mtk_ext_free(void* mem) {
#ifdef MTK_USE_RESERVED_EXT_MEM	
    uspace_ext_free(mem);
#else
    free(mem);
#endif
}


