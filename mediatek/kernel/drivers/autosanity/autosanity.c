#include <linux/module.h>

static int __init autosanity_init(void)
{
    printk("[%s]\n", __FUNCTION__);
    return 0;
}

static void __exit autosanity_exit(void)
{
    printk("[%s]\n", __FUNCTION__);
}

static uint do_autosanity;

static int param_get_autosanity(char *buffer, const struct kernel_param *kp)
{
    printk("[autosanity] failed!\n");
    panic("%s[%s] autosanity failed!!!\n", __FILE__, __FUNCTION__);
    return 0;
}

static const struct kernel_param_ops param_ops_autosanity = {
    .set = NULL,
    .get = &param_get_autosanity,
    .free = NULL,
};

param_check_uint(do_autosanity, &do_autosanity);
module_param_cb(do_autosanity, &param_ops_autosanity, &do_autosanity, S_IRUGO);
__MODULE_PARM_TYPE(do_autosanity, uint);

module_init(autosanity_init);
module_exit(autosanity_exit);

MODULE_LICENSE("GPL");


