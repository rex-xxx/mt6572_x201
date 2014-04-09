#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <stdint.h>
#include <assert.h>
#include <string.h>

#include <sys/ioctl.h>

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
    int ifd, ofd;
    int nr, nw = 0, total = 0;
    struct wav_header hdr;

    int opt;
    char * output = NULL;
    char * input = NULL;
    int bits_per_sample = 16;
    int sampling_rate = -1;
    int num_channels = -1;

    while ((opt = getopt(argc, argv, "o:c:b:s:")) != -1) {
        switch (opt) {
        case 'o':
            output = strdup(optarg);
            break;
        case 'c':
            num_channels = atoi(optarg);
            assert(num_channels == 1 || num_channels == 2);
            break;
        case 'b':
            bits_per_sample = atoi(optarg);
            break;
        case 's':
            sampling_rate = atoi(optarg);
            break;
        default: /* '?' */
            fprintf(stderr, "Usage: %s [-ooutfile] -c2 -b16 -s44100 infile\n",
                    argv[0]);
            exit(EXIT_FAILURE);
        }
    }

    assert(sampling_rate >= 0);
    assert(num_channels >= 0);

    FAILIF(!output, "Expecting an output file name\n");
    FAILIF(optind >= argc, "Expecting an input file name\n");

    input = argv[optind];

    printf("> input %s\n", input);
    printf("> output %s\n", output);
    printf("> bits per sample %d\n", bits_per_sample);
    printf("> sampling rate %d\n", sampling_rate);
    printf("> channels %d\n", num_channels);

    ofd = open(output, O_WRONLY | O_CREAT, 0777);
    FAILIF(ofd < 0, "could not open %s: %s\n", output, strerror(errno));

    ifd = open(input, O_RDONLY);
    FAILIF(ifd < 0, "could not open %s: %s\n", input, strerror(errno));

    lseek(ofd, sizeof(struct wav_header), SEEK_SET);

    do {
        nr = read(ifd, buffer, sizeof(buffer));
        FAILIF(nr < 0, "Could not read from input: %s\n", strerror(errno));
        if (!nr) {
            printf("done recording\n");
            break;
        }
        nw = write(ofd, buffer, nr);
        FAILIF(nw < 0, "Could not copy to output: %s\n", strerror(errno));
        FAILIF(nw != nr, "Mismatch nw = %d nr = %d\n", nw, nr);
        total += nw;
    } while (1);

    printf("writing WAV header\n");
    lseek(ofd, 0, SEEK_SET);
    init_wav_header(&hdr,
		    total * 8 / (num_channels * bits_per_sample),
		    bits_per_sample,
		    num_channels,
		    sampling_rate);

    FAILIF(write(ofd, &hdr, sizeof(hdr)) != sizeof(hdr),
           "Could not write WAV header: %s\n", strerror(errno));

    printf("done\n");
    return 0;
}
