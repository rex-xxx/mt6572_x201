
#include <graphite2/Segment.h>
#include <stdio.h>

typedef unsigned short uint16_t;

/* usage: ./simple fontfile.ttf string */
int main(int argc, char **argv)
{
    int rtl = 0;                /* are we rendering right to left? probably not */
    int pointsize = 12;         /* point size in points */
    int dpi = 96;               /* work with this many dots per inch */

    float textSize;

    char *pError;               /* location of faulty utf-8 */
    gr_font *font = NULL;
    size_t numCodePoints = 0;
    gr_segment * seg = NULL;
    const gr_slot *s;
    gr_face *face = gr_make_file_face(argv[1], 0);                              /*<1>*/
    if (!face) return 1;

    textSize = pointsize * dpi / 72.0f;
    textSize = 52.0;
    font = gr_make_font(textSize, face);                         /*<2>*/
    if (!font) return 2;

    uint16_t myanmarText[] = {0x1001, 0x102B, 0x101E, 0x102D, 0x1000, 0x1039, 0x1001, 0x102C,
            0x101E, 0x1012, 0x1039, 0x1013, 0x102B, 0x100A, 0x102D, 0x102F, 0x1011, 0x102F, 0x1036, 0x1038};
    uint16_t thaiText[] = {0xE01, 0xE49, 0xE33};
    uint16_t *text = myanmarText;


    numCodePoints = gr_count_unicode_characters(gr_utf16, text, NULL,
                (const void **)(&pError));                                      /*<3>*/
    if (pError) return 3;

    seg = gr_make_seg(font, face, 0, 0, gr_utf16, text, numCodePoints, rtl);  /*<4>*/
    if (!seg) return 3;

    for (s = gr_seg_first_slot(seg); s; s = gr_slot_next_in_segment(s))         /*<5>*/
        printf("Glyhp = %d, origin = (%f, %f), advance = (%f, %f)\n", gr_slot_gid(s),
                gr_slot_origin_X(s), gr_slot_origin_Y(s), gr_slot_advance_X(s, face, font), gr_slot_advance_Y(s, face, font));

    gr_seg_destroy(seg);
    gr_font_destroy(font);
    gr_face_destroy(face);
    return 0;
}
