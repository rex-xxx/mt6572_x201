#ifndef DFO_BOOT_DEFAULT_H
#define DFO_BOOT_DEFAULT_H

tag_dfo_boot dfo_boot_default =
{
    // name array
    {
        "MTK_ENABLE_MD1",
        "MTK_ENABLE_MD2",
        "MD1_SIZE",
        "MD2_SIZE",
        "MD1_SMEM_SIZE",
        "MD2_SMEM_SIZE",
        "MTK_MD1_SUPPORT",
        "MTK_MD2_SUPPORT"
    },

    // value array
    {
        1,
        0,
        0x01200000,
        0x00000000,
        0x00200000,
        0x00200000,
        3,
        4
    }
};

#endif
