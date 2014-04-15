#! /bin/sh
# Header Append for MTK-based Kernels to ensure proper header files are added to the Kernel Image

echo "Copying compiled kernel from out/arch/arm/boot..."
cp kernel/out/arch/arm/boot/zImage build/tools/zImage

# Creates a header file of 512 bytes using the Stock Kernel
cd build/tools
echo "Generating header from original MTK image..."
dd if=zImageStock of=header bs=512 count=1

# Takes the header file and copies it to the New zImage file
dd if=header of=zImageNew

# Appends the compiled zImage to the new zImage header
echo "Merging kernel files..."
dd if=zImage of=zImageNew seek=512
cd ~/mt6572_x201
cp build/tools/zImageNew kernel/out/arch/arm/boot/zImageNew
echo "Cleaning files..."
rm build/tools/zImageNew
rm build/tools/zImage
echo "Kernel image header successfully appended."
echo "New kernel image is located in kernel/out/arch/arm/boot."
