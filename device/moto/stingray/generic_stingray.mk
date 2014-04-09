# Add CameraBrowser for testing PTP support until Gallery supports it
PRODUCT_PACKAGES := CameraBrowser

$(call inherit-product, $(SRC_TARGET_DIR)/product/generic_no_telephony.mk)
$(call inherit-product, device/moto/stingray/device.mk)
$(call inherit-product-if-exists, vendor/moto/stingray/stingray-vendor.mk)

# Overrides
PRODUCT_DEVICE := stingray
PRODUCT_LOCALES += en_US
PRODUCT_MODEL := Xoom
PRODUCT_NAME := stingray
