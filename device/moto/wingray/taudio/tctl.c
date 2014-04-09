#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <assert.h>

#include <sys/ioctl.h>
#include <linux/cpcap_audio.h>
#include <linux/tegra_audio.h>

#define FAILIF(x, ...) do if (x) { \
    fprintf(stderr, __VA_ARGS__);  \
    exit(EXIT_FAILURE);            \
} while (0)

static char buffer[4096];

int
main(int argc, char *argv[])
{
    int opt, cfd;
    int output = -4;
    int input = -3;
    int volume = -1; /* max 15 */
    int in_volume = -1; /* max 31 */
    int record = -1; /* start 1, stop 0 */
    int use_dma = -1;
    int in_rate = -1;
    int in_channels = -1;

    while ((opt = getopt(argc, argv, "o::i::s:c:v::g::d:r:")) != -1) {
        switch (opt) {
        case 'o':
            if (optarg)
                output = atoi(optarg);
            else
                output = -5;
            break;
        case 'i':
            if (optarg)
                input = atoi(optarg);
            else
                input = -4;
            break;
        case 'v':
            if (optarg)
                volume = atoi(optarg);
            else
                volume = -2;
            break;
        case 's':
            in_rate = atoi(optarg);
            break;
        case 'c':
            in_channels = atoi(optarg);
            break;
        case 'g':
            if (optarg)
                in_volume = atoi(optarg);
            else
                in_volume = -2;
            break;
        case 'd':
            use_dma = atoi(optarg);
            break;
        case 'r':
            record = atoi(optarg);
            break;
        default: /* '?' */
            fprintf(stderr, "Unknown option\n");
            exit(EXIT_FAILURE);
        }
    }

    cfd = open("/dev/audio_ctl", O_RDWR);

    printf("cfd opened\n");

    FAILIF(cfd < 0, "could not open control: %s\n", strerror(errno));

    if (output > -4 && output < 4) {
        struct cpcap_audio_stream cfg;
        assert(!output); // 1 or 2 or 3 or -1 or -2 or -3
        if (output < 0) {
            cfg.id = (-output) - 1;
            cfg.on = 0;
            printf("set output %d to OFF\n", cfg.id);
        }
        else {
            cfg.id = output - 1;
            cfg.on = 1;
            printf("set output %d to ON\n", cfg.id);
        }
        FAILIF(ioctl(cfd, CPCAP_AUDIO_OUT_SET_OUTPUT, &cfg) < 0,
               "Cannot set output device %d: %s\n", cfg.id, strerror(errno));
    }
    else if (output == -5) {
        struct cpcap_audio_stream cfg;
        FAILIF(ioctl(cfd, CPCAP_AUDIO_OUT_GET_OUTPUT, &cfg) < 0,
               "Cannot get output device %d: %s\n", cfg.id, strerror(errno));
        printf("current output: %d, %s\n", cfg.id, (cfg.on ? "on" : "off"));
    }

    if (volume >= 0) {
        printf("set output volume\n");
        FAILIF(ioctl(cfd, CPCAP_AUDIO_OUT_SET_VOLUME, volume) < 0,
               "Cannot set volume to %d: %s\n", output, strerror(errno));
    }
    else if (volume == -2) {
        FAILIF(ioctl(cfd, CPCAP_AUDIO_OUT_GET_VOLUME, &volume) < 0,
               "Cannot get volume: %s\n", strerror(errno));
        printf("speaker volume: %d\n", volume);
    }

    if (in_volume >= 0) {
        printf("set input volume\n");
        FAILIF(ioctl(cfd, CPCAP_AUDIO_IN_SET_VOLUME, in_volume) < 0,
               "Cannot set input volume to %d: %s\n", output, strerror(errno));
    }
    else if (in_volume == -2) {
        FAILIF(ioctl(cfd, CPCAP_AUDIO_IN_GET_VOLUME, &in_volume) < 0,
               "Cannot get input volume: %s\n", strerror(errno));
        printf("microphone gain: %d\n", in_volume);
    }

    if (input > -3 && input < 3) {
        struct cpcap_audio_stream cfg;
        assert(!input); // 1 or 2 or -1 or -2
        if (input < 0) {
            cfg.id = (-input) - 1;
            cfg.on = 0;
            printf("set input %d to OFF\n", cfg.id);
        }
        else {
            cfg.id = input - 1;
            cfg.on = 1;
            printf("set input %d to ON\n", cfg.id);
        }
        FAILIF(ioctl(cfd, CPCAP_AUDIO_IN_SET_INPUT, &cfg) < 0,
               "Cannot set input device %d: %s\n", cfg.id, strerror(errno));
    }
    else if (input == -4) {
        struct cpcap_audio_stream cfg;
        FAILIF(ioctl(cfd, CPCAP_AUDIO_IN_GET_INPUT, &cfg) < 0,
               "Cannot get input device %d: %s\n", cfg.id, strerror(errno));
        printf("current input: %d, %s\n", cfg.id, (cfg.on ? "on" : "off"));
    }

    if (in_channels >= 0 || in_rate >= 0) {
        int recfd;
        struct tegra_audio_in_config cfg;

        printf("set input config\n");

        printf("opening audio input\n");
        recfd = open("/dev/audio1_in_ctl", O_RDWR);
        FAILIF(recfd < 0, "could not open for recording: %s\n", strerror(errno));

        printf("getting audio-input config\n");
        FAILIF(ioctl(recfd, TEGRA_AUDIO_IN_GET_CONFIG, &cfg) < 0,
               "could not get input config: %s\n", strerror(errno));
        if (in_channels >= 0)
            cfg.stereo = in_channels == 2;
        if (in_rate >= 0)
            cfg.rate = in_rate;
        printf("setting audio-input config (stereo %d, rate %d)\n", cfg.stereo, cfg.rate);
        FAILIF(ioctl(recfd, TEGRA_AUDIO_IN_SET_CONFIG, &cfg) < 0,
               "could not set input config: %s\n", strerror(errno));
        close(recfd);
    }

    if (use_dma >= 0) {
        int piofd = open("/sys/kernel/debug/tegra_audio/dma", O_RDWR);
        FAILIF(piofd < 0, "Could not open DMA/PIO toggle file: %s\n", strerror(errno));
        if (use_dma)
            FAILIF(write(piofd, "dma\n", sizeof("dma\n")) < 0,
                   "Could not set to DMA: %s\n", strerror(errno));
        else
            FAILIF(write(piofd, "dma\n", sizeof("pio\n")) < 0,
                   "Could not set to PIO: %s\n", strerror(errno));
    }

    if (record >= 0) {
        printf("opening audio input\n");
        int recfd = open("/dev/audio1_in_ctl", O_RDWR);
        printf("done opening audio input\n");
        FAILIF(recfd < 0, "could not open for recording: %s\n", strerror(errno));
        if (record) {
            printf("start recording\n");
            FAILIF(ioctl(recfd, TEGRA_AUDIO_IN_START) < 0,
                   "Could not start recording: %s\n", strerror(errno));
        } else {
            printf("stop recording\n");
            FAILIF(ioctl(recfd, TEGRA_AUDIO_IN_STOP) < 0,
                   "Could not stop recording: %s\n", strerror(errno));
        }
    }

    return 0;
}

