#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <stdint.h>
#include <assert.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <linux/cpcap_audio.h>
#include <linux/tegra_audio.h>

#define FAILIF(x, ...) do if (x) { \
    fprintf(stderr, __VA_ARGS__);  \
    exit(EXIT_FAILURE);            \
} while (0)

static char buffer[4096];

struct wav_header {
    char  riff[4];
    uint32_t chunk_size;
    char  format[4];

    char  subchunk1_id[4];
    uint32_t subchunk1_size;
    uint16_t audio_format;
    uint16_t num_channels;
    uint32_t sample_rate;
    uint32_t byte_rate;
    uint16_t block_align;
    uint16_t bits_per_sample;

    char  subchunk2_id[4];
    uint32_t subchunk2_size;
} __attribute__((packed));

static void init_wav_header(struct wav_header *hdr,
                       uint32_t num_samples,
                       uint16_t bits_per_sample,
                       int   channels,
                       uint32_t sample_rate)
{
    hdr->riff[0] = 'R';
    hdr->riff[1] = 'I';
    hdr->riff[2] = 'F';
    hdr->riff[3] = 'F';

    hdr->subchunk2_size = num_samples * channels * bits_per_sample / 8;

    hdr->chunk_size = 36 + hdr->subchunk2_size;
    hdr->format[0] = 'W';
    hdr->format[1] = 'A';
    hdr->format[2] = 'V';
    hdr->format[3] = 'E';

    hdr->subchunk1_id[0] = 'f';
    hdr->subchunk1_id[1] = 'm';
    hdr->subchunk1_id[2] = 't';
    hdr->subchunk1_id[3] = ' ';

    hdr->subchunk1_size = 16;
    hdr->audio_format = 1; /* PCM */
    hdr->num_channels = channels;
    hdr->sample_rate = sample_rate;
    hdr->byte_rate = sample_rate * channels * bits_per_sample / 8;
    hdr->block_align = channels * bits_per_sample / 8;
    hdr->bits_per_sample = bits_per_sample;

    hdr->subchunk2_id[0] = 'd';
    hdr->subchunk2_id[1] = 'a';
    hdr->subchunk2_id[2] = 't';
    hdr->subchunk2_id[3] = 'a';
}

int
main(int argc, char *argv[])
{
    int ifd, ifd_c, ofd, opt, cfd;
    const char *name;
    int nr, nw = 0, total = 0;
    int wave = 0;
    const int bits_per_sample = 16;
    int sampling_rate = -1;
    int num_channels = -1;

    struct tegra_audio_in_config cfg;
    struct wav_header hdr;

    while ((opt = getopt(argc, argv, "wc:s:")) != -1) {
        switch (opt) {
        case 'w':
            wave = 1;
            break;
        case 'c':
            num_channels = atoi(optarg);
            assert(num_channels == 1 || num_channels == 2);
            break;
        case 's':
            sampling_rate = atoi(optarg);
            break;
        default: /* '?' */
            fprintf(stderr,
                    "usage: %s [-w] [-s<rate>] [-c<chans>] <destfile>\n",
                    *argv);
            exit(EXIT_FAILURE);
        }
    }

    FAILIF(optind >= argc,
                    "usage: %s [-w] [-s<rate>] [-c<chans>] <destfile>\n",
                    *argv);

    name = argv[optind];

    printf("> recording into %s\n", name);
    printf("> sampling rate %d\n", sampling_rate);
    printf("> channels %d\n", num_channels);

    cfd = open("/dev/audio_ctl", O_RDWR);
    FAILIF(cfd < 0, "could not open control: %s\n", strerror(errno));
    if(sampling_rate > 0) {
        FAILIF(ioctl(cfd, CPCAP_AUDIO_IN_SET_RATE, sampling_rate),
            "Could not set input sampling rate: %s\n", strerror(errno));
    }

    ifd = open("/dev/audio1_in", O_RDWR);
    FAILIF(ifd < 0, "could not open input: %s\n", strerror(errno));

    ifd_c = open("/dev/audio1_in_ctl", O_RDWR);
    FAILIF(ifd < 0, "could not open input: %s\n", strerror(errno));

    printf("getting audio-input config\n");
    FAILIF(ioctl(ifd_c, TEGRA_AUDIO_IN_GET_CONFIG, &cfg) < 0,
           "could not get input config: %s\n", strerror(errno));

    if (num_channels >= 0 || sampling_rate >= 0) {
        if (num_channels >= 0)
            cfg.stereo = num_channels == 2;
//        if (sampling_rate >= 0)
//            cfg.rate = sampling_rate;
//  No sample rate conversion in driver
        cfg.rate = 44100;
        printf("setting audio-input config (stereo %d, rate %d)\n",
               cfg.stereo, cfg.rate);
        FAILIF(ioctl(ifd_c, TEGRA_AUDIO_IN_SET_CONFIG, &cfg) < 0,
               "could not set input config: %s\n", strerror(errno));
    }

    if (num_channels < 0) {
        num_channels = cfg.stereo ? 2 : 1;
        printf("> channels %d (from config)\n", num_channels);
    }

    if (sampling_rate < 0) {
        sampling_rate = cfg.rate;
        printf("> sampling rate %d (from config)\n", sampling_rate);
    }

    ofd = open(name, O_WRONLY | O_TRUNC | O_CREAT, 0666);
    FAILIF(ofd < 0, "could not open %s: %s\n", name, strerror(errno));

    if (wave)
        FAILIF(lseek(ofd, sizeof(struct wav_header), SEEK_SET) < 0,
               "seek error: %s\n", strerror(errno));

    do {
        errno = 0;
        nr = read(ifd, buffer, sizeof(buffer));
        FAILIF(nr < 0, "input read error: %s\n", strerror(errno));

        if (!nr) {
            printf("done recording\n");
            break;
        }

        printf("in %d\n", nr);

        nw = write(ofd, buffer, nr);
        FAILIF(nw < 0, "Could not copy to output: %s\n", strerror(errno));
        FAILIF(nw != nr, "Mismatch nw = %d nr = %d\n", nw, nr);
        total += nw;
    } while (1);

    if (wave) {
        printf("writing WAV header\n");
        lseek(ofd, 0, SEEK_SET);
        init_wav_header(&hdr,
                        total * 8 / (num_channels * bits_per_sample),
                        bits_per_sample,
                        num_channels,
                        sampling_rate);
        FAILIF(write(ofd, &hdr, sizeof(hdr)) != sizeof(hdr),
               "Could not write WAV header: %s\n", strerror(errno));
    }

    printf("done\n");
    return 0;
}
