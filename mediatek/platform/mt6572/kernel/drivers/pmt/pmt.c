#include <linux/module.h>
#include <linux/moduleparam.h>
#include <asm/uaccess.h>   /*set_fs get_fs mm_segment_t*/
#include <linux/kernel.h>	/* printk() */
#include <linux/slab.h>		/* kmalloc() */
#include <linux/types.h>	/* size_t */
#include <linux/proc_fs.h>  /*proc*/
#include <linux/cdev.h>

#include "partition_define.h"
//#include "dumchar.h"		/* local definitions */
#include "pmt.h"
#include <linux/mmc/host.h>
#include <sd_misc.h>
//#include "../../platform/mt6589/kernel/drivers/mmc-host/mt_sd.h"
#include <linux/genhd.h>


#ifdef MTK_EMMC_SUPPORT
typedef struct _DM_PARTITION_INFO_x
{
    char part_name[MAX_PARTITION_NAME_LEN];             /* the name of partition */
    unsigned long long start_addr;                                  /* the start address of partition */
    unsigned long long part_len;                                    /* the length of partition */
    unsigned char part_visibility;                              /* part_visibility is 0: this partition is hidden and CANNOT download */
                                                        /* part_visibility is 1: this partition is visible and can download */                                            
    unsigned char dl_selected;                                  /* dl_selected is 0: this partition is NOT selected to download */
                                                        /* dl_selected is 1: this partition is selected to download */
} DM_PARTITION_INFO_x;

typedef struct {
    unsigned int pattern;
    unsigned int part_num;                              /* The actual number of partitions */
    DM_PARTITION_INFO_x part_info[PART_MAX_COUNT];
} DM_PARTITION_INFO_PACKET_x;

typedef struct {
	int sequencenumber:8;
	int tool_or_sd_update:4;
	int pt_next:4;
	int mirror_pt_dl:4;   //mirror download OK
	int mirror_pt_has_space:4;
	int pt_changed:4;
	int pt_has_space:4;
} pt_info_x;

unsigned long long User_region_Header_Byte = 0;
unsigned long long User_Region_Size_Byte = 0;
unsigned long long MBR_START_ADDR = 0;


static pt_resident *lastest_part;
static pt_info_x pi;
int pmt_done = 0;
unsigned long long emmc_size = 0;

#define MSDOS_LABEL_MAGIC1	0x55
#define MSDOS_LABEL_MAGIC2	0xAA

static inline int
msdos_magic_present(unsigned char *p)
{
	return (p[0] == MSDOS_LABEL_MAGIC1 && p[1] == MSDOS_LABEL_MAGIC2);
}

static int load_pt_from_fixed_addr(u8 *buf);
static int update_MBR_or_EBR(int px, u64 start_addr, u64 length);
static int init_region_info(void);
extern int eMMC_rw_x(loff_t addr,u32  *buffer, int host_num, int iswrite,u32 totalsize, int transtype, Region part);
extern u32 msdc_get_capacity(int get_emmc_total);
extern int msdc_get_reserve(void);
extern int mt65xx_mmc_change_disk_info(unsigned int px, unsigned int addr, unsigned int size);

static int init_region_info(void)
{

	User_Region_Size_Byte = (u64)msdc_get_capacity(0)*512;
	emmc_size = (u64)msdc_get_capacity(1)*512;
	User_region_Header_Byte = emmc_size - User_Region_Size_Byte;
	printk("[Dumchar_init_region_info]emmc_size = 0x%llx, user region size = 0x%llx, header size = 0x%llx\n",emmc_size,User_Region_Size_Byte,User_region_Header_Byte);
	return 0;
	
	
}

int init_pmt(void)
{
	int ret = 0;
    int i = 0;

    printk("[%s]start\n", __func__);
    ret = init_region_info();

	if (pmt_done) {
		printk("pmt has been initialised, so skip\n");
		return 0;
	}

    //init region info to get user region size.
	lastest_part = kmalloc(PART_MAX_COUNT * sizeof(pt_resident), GFP_KERNEL);
	if (!lastest_part) {
		ret = -ENOMEM;
		printk("init_pmt: malloc lastest_part fail\n");
		goto fail_malloc;
	}

    memset(lastest_part,0, PART_MAX_COUNT * sizeof(pt_resident));
    memset(&pi,0,sizeof(pt_info));

    ret = load_pt_from_fixed_addr((u8 *)lastest_part);
    if (ret != DM_ERR_OK) { 
        //and valid mirror last download or first download 
       printk("can not find pmt,use default part info\n");
    } else {
       printk("find pt\n");
       for (i = 0; i < PART_MAX_COUNT; i++) {  
	   		if ((lastest_part[i].name[0] == 0x00) || (lastest_part[i].name[0] == 0xff))
				break;
            printk("part %s size %llx %llx\n", lastest_part[i].name, 
                lastest_part[i].offset, lastest_part[i].size);
			PartInfo[i].start_address = lastest_part[i].offset;
			PartInfo[i].size= lastest_part[i].size;
		    /*	if(lastest_part[i].size == 0)
				break;*/
        }
        printk("find pt %d\n",i);
    }
    printk("[%s]done\n", __func__);

	pmt_done = 1;
	ret = 0;
fail_malloc: 
	return ret;

}
EXPORT_SYMBOL(init_pmt);

#define PMT_REGION_SIZE     (0x1000)
#define PMT_REGION_OFFSET   (0x100000)

#define PMT_VER_V1          ("1.0")
#define PMT_VER_SIZE        (4)

static unsigned char pmt_buf[PMT_REGION_SIZE];

static int load_pt_from_fixed_addr(u8 * buf)
{
    int reval = ERR_NO_EXIST;
    u64 pt_start;
    u64 mpt_start;

    int pt_size = PMT_REGION_SIZE;

    pt_start = emmc_size - PMT_REGION_OFFSET - 0x600000;		
    mpt_start = pt_start + PMT_REGION_SIZE;

    printk(KERN_NOTICE "============func=%s===scan pmt from %llx=====\n", __func__,pt_start);

    reval = eMMC_rw_x(pt_start, (u32 *)pmt_buf, 0, 0, PMT_REGION_SIZE, 1, USER);
    if (reval) {
        printk(KERN_ERR "read pt error\n");
        goto try;
    }

    if (is_valid_pt(pmt_buf)) {
        if (!memcmp(pmt_buf + PT_SIG_SIZE, PMT_VER_V1, PMT_VER_SIZE)) {
            if (is_valid_pt(&pmt_buf[pt_size - PT_SIG_SIZE])) {
                memcpy(buf, pmt_buf + PT_SIG_SIZE + PMT_VER_SIZE, PART_MAX_COUNT * sizeof(pt_resident));
                reval = DM_ERR_OK;
                printk(KERN_NOTICE "find pt at %llx\n", pt_start);
                return reval;
            } else {
                reval = ERR_NO_EXIST;
                printk(KERN_ERR "invalid tail pt format\n");
            }
        } else {
            reval = ERR_NO_EXIST;
            printk(KERN_ERR "invalid pt version %s\n", pmt_buf + PT_SIG_SIZE);
        }
    }

try:
    reval = eMMC_rw_x(mpt_start, (u32 *)pmt_buf, 0, 0, PMT_REGION_SIZE, 1, USER);
    if (reval) {
        printk(KERN_ERR "read mpt error\n");
        reval = ERR_NO_EXIST;
        return reval;
    }

    if (is_valid_mpt(pmt_buf)) {
        if (!memcmp(pmt_buf + PT_SIG_SIZE, PMT_VER_V1, PMT_VER_SIZE)) {
            if (is_valid_mpt(&pmt_buf[pt_size - PT_SIG_SIZE])) {
                memcpy(buf, pmt_buf + PT_SIG_SIZE + PMT_VER_SIZE, PART_MAX_COUNT * sizeof(pt_resident));
                reval = DM_ERR_OK;
                printk(KERN_NOTICE "find mpt at %llx\n", mpt_start);
                return reval;
            } else {
                reval = ERR_NO_EXIST;
                printk(KERN_ERR "invalid tail mpt format\n");
            }
        } else {
            reval = ERR_NO_EXIST;
            printk(KERN_ERR "invalid mpt version %s\n", pmt_buf + PT_SIG_SIZE);
        }
    }

    return reval;
}

static int new_pmt(pt_resident *new_part, int table_size)
{
	int ret = -1;
	u64 mpt_addr = 0;
	char sig_buf[PT_SIG_SIZE];

    mpt_addr = emmc_size - PMT_REGION_OFFSET - 0x600000 + PMT_REGION_SIZE;

	pi.pt_changed = 1;
	pi.tool_or_sd_update = 2;
	pi.sequencenumber += 1;

	memset(pmt_buf, 0x00, PMT_REGION_SIZE);
	
	ret = eMMC_rw_x(mpt_addr, (u32 *)pmt_buf, 0, 1, PMT_REGION_SIZE, 1, USER);
	if (ret) {
        printk("clear mpt error\n");
		goto end;
	}
	
	*(int *)sig_buf = MPT_SIG;
	memcpy(pmt_buf, &sig_buf, PT_SIG_SIZE);
    memcpy(pmt_buf + PT_SIG_SIZE, PMT_VER_V1, PMT_VER_SIZE);
	memcpy(pmt_buf + PT_SIG_SIZE + PMT_VER_SIZE, &new_part[0], (table_size * sizeof(pt_resident)));
	memcpy(pmt_buf + PMT_REGION_SIZE - PT_SIG_SIZE - sizeof(pi), &pi, sizeof(pi));
	memcpy(pmt_buf + PMT_REGION_SIZE - PT_SIG_SIZE, &sig_buf, PT_SIG_SIZE);

	ret = eMMC_rw_x(mpt_addr,(u32 *)pmt_buf, 0, 1, PMT_REGION_SIZE, 1, USER);
    if(ret) {
		printk("write mpt error\n");
	}

end:
	return ret;
}

static int update_pmt(pt_resident *new_part, int table_size)
{
	int ret = -1;
	u64 pt_addr = 0;
	char sig_buf[PT_SIG_SIZE];
	
    pt_addr = emmc_size - PMT_REGION_OFFSET - 0x600000;

	if ((pi.pt_changed != 1) && (pi.pt_has_space == 1)) {
		printk("pt may be not update\n");
		return 0;
	}

	memset(pmt_buf, 0x00, PMT_REGION_SIZE);

	ret = eMMC_rw_x(pt_addr, (u32 *)pmt_buf, 0, 1, PMT_REGION_SIZE, 1, USER);
	if (ret) {
		printk("clear pt error\n");
		goto end;
	}
	
	*(int *)sig_buf = PT_SIG;
	memcpy(pmt_buf, &sig_buf, PT_SIG_SIZE);
    memcpy(pmt_buf + PT_SIG_SIZE, PMT_VER_V1, PMT_VER_SIZE);
	memcpy(pmt_buf + PT_SIG_SIZE + PMT_VER_SIZE, &new_part[0], (table_size * sizeof(pt_resident)));
	memcpy(pmt_buf + PMT_REGION_SIZE - PT_SIG_SIZE - sizeof(pi), &pi, sizeof(pi));
	memcpy(pmt_buf + PMT_REGION_SIZE - PT_SIG_SIZE, &sig_buf, PT_SIG_SIZE);

	ret = eMMC_rw_x(pt_addr,(u32 *)pmt_buf, 0, 1, PMT_REGION_SIZE, 1, USER);
    if (ret) {
		printk("write pt error\n");
	}

end:
	return ret;
}

int read_pmt(void __user *arg)
{
	printk("read_pmt\n");
	if(copy_to_user(arg, lastest_part, sizeof(pt_resident) * PART_MAX_COUNT))
		return -EFAULT;
	return 0;
}
EXPORT_SYMBOL(read_pmt);

int write_pmt(void __user *arg)
{
	int ret = 0;
	int i;
	int table_size =0;
	pt_resident *new_part;

	new_part = kmalloc(PART_MAX_COUNT * sizeof(pt_resident), GFP_KERNEL);
	if (!new_part) {
        ret = -ENOMEM;
        printk("write_pmt: malloc new_part fail\n");
        goto fail_malloc;
	}

	if (copy_from_user(new_part, arg, PART_MAX_COUNT * sizeof(pt_resident))) {
        ret = -EFAULT;
        goto end;
    }

	for (i = 0; i < PART_MAX_COUNT; i++) {
		if (new_part[i].size == 0)
			break;
	}

	if (i == 0)
		return 0;
	
	table_size = i + 1;

	printk("write table size %d\n",table_size);

	ret = new_pmt(new_part, table_size);
	ret = update_pmt(new_part, table_size);

end:
	kfree(new_part);
fail_malloc:
	return ret;
}
EXPORT_SYMBOL(write_pmt);

static int sd_upgrade_proc_write(struct file*file, const char*buffer,unsigned long count,void *data)
{
	DM_PARTITION_INFO_PACKET_x *pmtctl;
	pt_resident *new_part;
	int part_num,change_index,i;
	int ret=0;
	int pt_change = 0;
	int pt_change_tb[PART_MAX_COUNT];

	memset(&pt_change_tb,0x00,PART_MAX_COUNT*sizeof(int));
	
	pmtctl = kmalloc(sizeof(DM_PARTITION_INFO_PACKET_x),GFP_KERNEL);
	if (!pmtctl) {
			ret = -ENOMEM;
			printk("sd_upgrade_proc_write: malloc pmtctl fail\n");
			goto fail_malloc;
	}
	memset(pmtctl,0x00,sizeof(DM_PARTITION_INFO_PACKET_x));
	
	new_part = kmalloc(PART_MAX_COUNT*sizeof(pt_resident),GFP_KERNEL);
	if (!new_part) {
			ret = -ENOMEM;
			printk("sd_upgrade_proc_write: malloc new_part fail\n");
			goto fail_malloc;
	}
	memset(new_part,0x00,PART_MAX_COUNT*sizeof(pt_resident));
	
	if(copy_from_user(pmtctl,buffer,sizeof(DM_PARTITION_INFO_PACKET_x))){
		ret = -EFAULT;
		goto end;
		
	}
	
//1. copy new part
	for(i=0;i<PART_MAX_COUNT;i++)
	{
		memcpy(new_part[i].name,pmtctl->part_info[i].part_name,MAX_PARTITION_NAME_LEN);
		new_part[i].offset=pmtctl->part_info[i].start_addr;
		new_part[i].size=pmtctl->part_info[i].part_len;
		new_part[i].mask_flags=0;
		//MSG (INIT, "DM_PARTITION_INFO_PACKET %s size %x %x \n",dm_part->part_info[part_num].part_name,dm_part->part_info[part_num].part_len,part_num);
		printk ("[SD_UPGRADE]new_pt %s size %llx \n",new_part[i].name,new_part[i].size);
		if(pmtctl->part_info[i].part_len ==0)
		{
			printk ("[SD_UPGRADE]new_pt last %d \n",i);
			break;
		}
	}
	part_num = i+1;
	printk("[SD_UPGRADE]table size %d\n",part_num);
//2. compare new part and lastest part.
	for(change_index=0;change_index<part_num;change_index++)
	{
		if((new_part[change_index].size!=lastest_part[change_index].size)||(new_part[change_index].offset!=lastest_part[change_index].offset))
		{
			printk ("[SD_UPGRADE]new_pt %d size changed from %llx to %llx\n",change_index,lastest_part[change_index].size,new_part[change_index].size);
			pt_change =1;
			pt_change_tb[change_index]=1;
			if((pmtctl->part_info[change_index].dl_selected == 0) && (pmtctl->part_info[change_index].part_visibility == 1))
			{
				printk("[SD_UPGRADE]please download all image\n");
				ret = -1;
				goto end;
			}
		}
	}
	if(!pt_change)
	{
		printk("[SD_UPGRADE]layout can not change,skip update PMT/MBR\n");
		goto end;
	}
//3. update PMT
		
	ret = new_pmt(new_part,part_num);
	if(ret){
		printk("[SD_UPGRADE] update m-pt fail\n");
		goto end;

	}
	ret = update_pmt(new_part,part_num);
	if(ret){
		printk("[SD_UPGRADE] update pt fail\n");
		goto end;

	}
	printk("[SD_UPGRADE] update PMT sucess\n");
//
		for(i=0;i<=part_num;i++){
			if((pt_change_tb[i]==1) &&(new_part[i].size == 0)){
					new_part[i].size = User_Region_Size_Byte - new_part[i].offset + MBR_START_ADDR - msdc_get_reserve()*512;
			}
		}
//4. update MBR/EBR
	for(i=0;i<=part_num;i++){
		if(pt_change_tb[i]==1){
			if(PartInfo[i].partition_idx!=0){
				printk("update p %d %llx %llx\n",PartInfo[i].partition_idx,new_part[i].offset-MBR_START_ADDR,new_part[i].size);
				ret = update_MBR_or_EBR(PartInfo[i].partition_idx,new_part[i].offset-MBR_START_ADDR,new_part[i].size);
				if(ret){
					printk("[SD_UPGRADE]update_MBR_or_EBR fail\n");
					goto end;
				}
			}
		}
	}
	printk("[SD_UPGRADE] update  MBR/EBR sucess\n");
//5. change part device offset and size.

	for(i=0;i<=part_num;i++){
		if(pt_change_tb[i]==1){
			if(PartInfo[i].partition_idx!=0){
				printk("update p %d %llx %llx\n",PartInfo[i].partition_idx,new_part[i].offset-MBR_START_ADDR,new_part[i].size);
				ret = mt65xx_mmc_change_disk_info(PartInfo[i].partition_idx,(u32)((new_part[i].offset-MBR_START_ADDR)/512),(u32)((new_part[i].size)/512));
				if(ret){
					printk("[SD_UPGRADE]update  part device offset and size fail\n");
					goto end;
				}
			}
		}
	}
	printk("[SD_UPGRADE] update  part device offset and size sucess\n");

end:
	kfree(pmtctl);
	kfree(new_part);
fail_malloc:
	if(ret)
		return ret;
	else
		return count;

	
}

static int update_MBR_or_EBR(int px, u64 start_addr, u64 length)
{
	int i,ret,j;
	int found_mbr = 0;
	loff_t update_addr = 0;
	int index_in_mbr = 0;
	int mbr_index = 0;
	char *change_pt_name = NULL;
	struct partition *p;
	u8 *page_buf = NULL;
	ret =0;
		
	page_buf = kmalloc(512, GFP_KERNEL);
	if (!page_buf) {
		ret = -ENOMEM;
		printk("update_MBR_or_EBR: malloc page_buf fail\n");
		goto fail_malloc;
	}
	//data -1MB
/*	for(i=0;i<PART_NUM;i++){
		if((PartInfo[i].partition_idx == px)&&((!strncmp(PartInfo[i].name,"usrdata",7))||(!strncmp(PartInfo[i].name,"sec_ro",6))||(!strncmp(PartInfo[i].name,"android",7))||(!strncmp(PartInfo[i].name,"cache",5)))){
			printk("update %s,need reduce 1MB in MBR\n",PartInfo[i].name);
			length -= 0x100000;
		}
	}*/
	
	
	//find px in which mbr/ebr.
	for(i=0;i<MBR_COUNT;i++){
		for(j=0;j<SLOT_PER_MBR;j++){
			if(MBR_EBR_px[i].part_index[j]==px){
				found_mbr = 1;
				change_pt_name = MBR_EBR_px[i].part_name;
				index_in_mbr = j;
				mbr_index = i;
			}
		}
	}
	if(found_mbr!=1){
		printk("p%d can not be found in mbr\n",px);
		ret = -1;
		goto end;
	}
	printk("update %s\n",change_pt_name);

	for(i=0; i<PART_NUM;i++){
		if(!strcmp(change_pt_name,PartInfo[i].name)){
			update_addr = PartInfo[i].start_address - MBR_START_ADDR;
			printk("update %s addr %llx\n",change_pt_name,update_addr);
			break;
		}
	}
	if(i==PART_MAX_COUNT){
		printk("can not find %s\n",change_pt_name);
		ret = -1;
		goto end;
	}
	ret = eMMC_rw_x(update_addr,(u32*)page_buf,0,0,512,1,USER);
	if(ret){
		printk("read %s error\n",change_pt_name);
		goto end;
	}
	if (!msdos_magic_present(page_buf + 510)) {
		printk("read MBR/EBR fail\n");
		ret = -1;
		goto end;
	}
	p = (struct partition *) (page_buf + 0x1be);

	for(i=0;i<4;i++){
		if(MBR_EBR_px[mbr_index].part_index[i]!=0){
			printk("p%d: %x %x\n",MBR_EBR_px[mbr_index].part_index[i],p[i].start_sect,p[i].nr_sects);
			if(i==index_in_mbr){
				printk("p%d: change to %x %x\n",MBR_EBR_px[mbr_index].part_index[i],(u32)((start_addr-update_addr)/512),(u32)(length/512));
				p[i].start_sect = (u32)((start_addr-update_addr)/512);
				p[i].nr_sects = (u32)(length/512);
			}
		}
	}

	ret = eMMC_rw_x(update_addr,(u32*)page_buf,0,1,512,1,USER);
	if(ret){
		printk("write %s error\n",change_pt_name);
		goto end;
	}
end:
	kfree(page_buf);
fail_malloc:
	return ret;
}


#define  PMT_MAGIC   'p'
#define PMT_READ        _IOW(PMT_MAGIC, 1, int)
#define PMT_WRITE       _IOW(PMT_MAGIC, 2, int)

static long pmt_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{
    long err;
    void __user *argp = (void __user *)arg;

    switch (cmd)
    {
        case PMT_READ:
            err = read_pmt(argp);
            break;
        case PMT_WRITE:
            err = write_pmt(argp);
            break;
        default:
            err= -EINVAL;
    }
    return err;
}

static unsigned int major;
static struct class *pmt_class;
static struct cdev *pmt_cdev;
static struct file_operations pmt_cdev_ops = {
    .owner = THIS_MODULE,
    .unlocked_ioctl = pmt_ioctl,
};

static void create_pmt_cdev(void)
{
    int err;
    dev_t devno;
    struct device *pmt_dev;

    err = alloc_chrdev_region(&devno, 0, 1, "pmt");
    if (err) {
        printk("[%s]fail to alloc devno\n", __func__);
        goto fail_alloc_devno;
    }
    
    major = MAJOR(devno);

    pmt_cdev = cdev_alloc();
    if (!pmt_cdev) {
        printk("[%s]fail to alloc cdev\n", __func__);
        goto fail_alloc_cdev;
    }

    pmt_cdev->owner = THIS_MODULE;
    pmt_cdev->ops = &pmt_cdev_ops;

    err = cdev_add(pmt_cdev, devno, 1);
    if (err) {
        printk("[%s]fail to add cdev\n", __func__);
        goto fail_add_cdev;
    }

    pmt_class = class_create(THIS_MODULE, "pmt");
    if (IS_ERR(pmt_class)) {
        printk("[%s]fail to create class pmt\n", __func__);
        goto fail_create_class;
    }
    
    pmt_dev = device_create(pmt_class, NULL, devno, NULL, "pmt");
    if (IS_ERR(pmt_dev)) {
        printk("[%s]fail to create class pmt\n", __func__);
        goto fail_create_device;
    }

    return;

fail_create_device:
    class_destroy(pmt_class);
fail_create_class:
fail_add_cdev:
    cdev_del(pmt_cdev);
fail_alloc_cdev:
    unregister_chrdev_region(devno, 1);
fail_alloc_devno:
    return;
}

static void remove_pmt_cdev(void)
{
    device_destroy(pmt_class, MKDEV(major, 0));
    class_destroy(pmt_class);
    cdev_del(pmt_cdev);
    unregister_chrdev_region(MKDEV(major, 0), 1);
}

static int __init pmt_interface_init(void)
{
	struct proc_dir_entry *sd_upgrade_proc = NULL;

	sd_upgrade_proc = create_proc_entry("sd_upgrade", 0600, NULL);
	if (sd_upgrade_proc) {
		sd_upgrade_proc->write_proc = sd_upgrade_proc_write;
		printk( "pmt_interface_init: register /proc/sd_upgrade success %p\n",sd_upgrade_proc->write_proc);
	} else {
		printk( "pmt_interface_init: unable to register /proc/sd_upgrade\n");
	}

    create_pmt_cdev();

    return 0;
}

static void __exit pmt_interface_cleanup(void)
{
	remove_proc_entry("sd_upgrade", NULL);

    remove_pmt_cdev();
}

module_init(pmt_interface_init);
module_exit(pmt_interface_cleanup);

#endif

MODULE_LICENSE("GPL");
MODULE_DESCRIPTION("MediaTek Partition Table Management Driver(eMMC)");
MODULE_AUTHOR("Jiequn.Chen <Jiequn.Chen@mediatek.com>");
