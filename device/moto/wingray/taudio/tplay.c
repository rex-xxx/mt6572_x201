#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#include <sys/ioctl.h>
#include <linux/tegra_audio.h>

#define FAILIF(x, ...) do if (x) { \
    fprintf(stderr, __VA_ARGS__);  \
    exit(EXIT_FAILURE);            \
} while (0)

int
main(int argc, char *argv[])
{
    int ifd, ofd, ofd_c;
    int nr, nw;
    int opt;
//    struct tegra_audio_buf_config config;
    char *name;
    char *buffer;
    int len = -1;
//    struct tegra_audio_error_counts errors, errors_tot;

    while ((opt = getopt(argc, argv, "n:")) != -1) {
        switch (opt) {
        case 'n':
            len = atoi(optarg);
            break;
        default: /* '?' */
            fprintf(stderr, "Usage: %s [-n<len>] name\n",
                    argv[0]);
            exit(EXIT_FAILURE);
        }
    }

    name = argv[optind];
    FAILIF(!name, "Expecting a file to play!\n");

    printf("file to play: [%s]\n", name);

    ifd = open(name, O_RDONLY);
    FAILIF(ifd < 0, "could not open %s: %s\n", name, strerror(errno));

    ofd = open("/dev/audio0_out", O_RDWR);
    FAILIF(ofd < 0, "could not open output: %s\n", strerror(errno));

    ofd_c = open("/dev/audio0_out_ctl", O_RDWR);
    FAILIF(ofd_c < 0, "could not open output control: %s\n", strerror(errno));

#if 0
    FAILIF(ioctl(ofd_c, TEGRA_AUDIO_OUT_GET_BUF_CONFIG, &config) < 0,
           "Could not get output config: %s\n", strerror(errno));
#endif

    if (len < 0)
        len = 4096;

    printf("write length: %d\n", len);

    buffer = malloc(len);
    FAILIF(!buffer, "Could not allocate %d bytes!\n", len);

//    memset(&errors_tot, 0, sizeof(errors_tot));
    do {
        nr = read(ifd, buffer, len);
        if (!nr) {
            printf("EOF\n");
            break;
        }
        FAILIF(nr < 0, "Could not read from %s: %s\n", name, strerror(errno));
        nw = write(ofd, buffer, nr);
        FAILIF(nw < 0, "Could not copy to output: %s\n", strerror(errno));
        FAILIF(nw != nr, "Mismatch nw = %d nr = %d\n", nw, nr);

#if 0
        FAILIF(ioctl(ofd_c, TEGRA_AUDIO_OUT_GET_ERROR_COUNT, &errors) < 0,
               "Could not get error count: %s\n", strerror(errno));

        if (errors.late_dma || errors.full_empty) {
            printf("out %d (%d late, %d underrun errors)\n", nw,
                   errors.late_dma, errors.full_empty);
            errors_tot.late_dma += errors.late_dma;
            errors_tot.full_empty += errors.full_empty;
        }
        else
#endif
            printf("out %d\n", nw);

    } while (1);

#if 0
    printf("played with %d late, %d underflow errors\n",
           errors_tot.late_dma, errors_tot.full_empty);
#endif
    return 0;
}

