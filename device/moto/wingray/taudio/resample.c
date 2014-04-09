#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>

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

static const int divs_8000[] = { 5, 6, 6, 5 };
static const int divs_11025[] = { 4 };
static const int divs_22050[] = { 2 };
static const int divs_44100[] = { 1 };

int downsample(const int16_t *in, int16_t *out, int len, int *consumed,
               const int *divs, int divs_len,
               int out_stereo)
{
    int i, j, lsum, rsum;
    int di, div;
    int oi;

    i = 0;
    oi = 0;
    di = 0;
    div = divs[0];
    while (i + div * 2 <= len) {
//        printf("div %d, i %d, oi %d\n", div, i, oi);
        for (j = 0, lsum = 0, rsum = 0; j < div; j++) {
            lsum += in[i + j * 2];
            rsum += in[i + j * 2 + 1];
        }
        if (!out_stereo)
            out[oi] = (lsum + rsum) / (div * 2);
        else {
            out[oi] = lsum / div;
            out[oi + 1] = rsum / div;
        }

        oi += out_stereo + 1;
        i += div * 2;
        div = divs[++di % divs_len];
    }

//    printf("done: i %d, len %d, oi %d\n", i, len, oi);
    *consumed = i;
    return oi;
}

#define FAILIF(x, ...) do if (x) { \
    fprintf(stderr, __VA_ARGS__);  \
    exit(EXIT_FAILURE);            \
} while (0)

int main(int argc, char **argv)
{
    int opt, ifd, ofd;
    int new_rate = -1;
    const int *divs;
    int divs_len;
    int consumed;
    int channels = -1;
    char *input = NULL;
    char *output = NULL;
    int nr, nr_out, nw;
    int put_header = 0;
    int total = 0;
    const int bits_per_sample = 16;

    struct wav_header src_hdr, dst_hdr;

    int16_t buf[2048];

    while ((opt = getopt(argc, argv, "o:s:c:w")) != -1) {
        switch (opt) {
        case 'o':
            FAILIF(output != NULL, "Multiple output files not supported\n");
            output = strdup(optarg);
            break;
        case 's':
            new_rate = atoi(optarg);
            break;
        case 'c':
            channels = atoi(optarg);
            break;
        case 'w':
            put_header = 1;
            break;
        default: /* '?' */
            fprintf(stderr, "usage: %s -o<outfile> -s<sampling> -c<channels>\n",
                *argv);
            fprintf(stderr, "usage: %s -o<outfile> -s<sampling> -c<channels>\n",
                *argv);
            exit(EXIT_FAILURE);
        }
    }

    FAILIF(channels != 1 && channels != 2, "-c value must be 1 or 2\n");

    switch(new_rate) {
    case 8000:
        divs = divs_8000;
        divs_len = 4;
        break;
    case 11025:
        divs = divs_11025;
        divs_len = 1;
        break;
    case 22050:
        divs = divs_22050;
        divs_len = 1;
        break;
    case 44100:
        divs = divs_44100;
        divs_len = 1;
        break;
    default:
        FAILIF(1, "rate %d is not supported\n", new_rate);
    }

    FAILIF(!output, "Expecting an output file name\n");
    FAILIF(optind >= argc, "Expecting an input file name\n");

    input = argv[optind];

    printf("input file [%s]\n", input);
    printf("output file [%s]\n", output);
    printf("new rate: [%d]\n", new_rate);

    ifd = open(input, O_RDONLY);
    FAILIF(ifd < 0, "Could not open %s: %s\n", input, strerror(errno));
    ofd = open(output, O_WRONLY | O_CREAT | O_TRUNC, 0666);
    FAILIF(ofd < 0, "Could not open %s: %s\n", output, strerror(errno));

    if (strstr(input, ".wav")) {
        FAILIF(read(ifd, &src_hdr, sizeof(src_hdr)) != sizeof(src_hdr),
            "Failed to read input WAV header: %s\n",
            strerror(errno));
        FAILIF(src_hdr.audio_format != 1,
            "Expecting PCM encoding\n");
        FAILIF(src_hdr.sample_rate != 44100,
            "Expecting 44.kHz files\n");
        FAILIF(src_hdr.num_channels != 2,
            "Expecting 2-channel files\n");
        FAILIF(src_hdr.bits_per_sample != bits_per_sample,
            "Expecting 16-bit PCM files\n");
    }

    if (put_header)
        FAILIF(lseek(ofd, sizeof(struct wav_header), SEEK_SET) < 0,
            "seek error in %s: %s\n", output, strerror(errno));

    consumed = 0;
    while (1) {
        nr = read(ifd, buf + consumed, sizeof(buf) - consumed);
        FAILIF(nr < 0, "could not read from %s: %s\n", input, strerror(errno));
        if (!nr) {
            printf("done\n");
            break;
        }
        nr += consumed;

//      printf("resampling %d samples\n", nr / 2);
        nr_out = downsample(buf, buf, nr / 2, &consumed, divs, divs_len, channels == 2);
        consumed *= 2;
//      printf("done: %d samples were generated (consumed %d out of %d bytes)\n", nr_out, consumed, nr);

        if (consumed < nr) {
            memcpy(buf, buf + consumed, nr - consumed);
            consumed = nr - consumed;
//          printf("copied %d bytes to front\n", consumed);
        }
        else consumed = 0;

        nr_out *= 2;
        nw = write(ofd, buf, nr_out);
        FAILIF(nw < 0, "could not write to %s: %s\n", output, strerror(errno));
        FAILIF(nw != nr_out, "mismatch, generated %d, wrote %d bytes\n", nr_out, nw);
        total += nw;
    }

    if (put_header) {
        printf("writing WAV header\n");
        lseek(ofd, 0, SEEK_SET);
        init_wav_header(&dst_hdr,
            total * 8 / (channels * bits_per_sample),
            bits_per_sample,
            channels,
            new_rate);
        FAILIF(write(ofd, &dst_hdr, sizeof(dst_hdr)) != sizeof(dst_hdr),
            "Could not write WAV header: %s\n", strerror(errno));
    }

    close(ifd);
    close(ofd);

    return 0;
}
