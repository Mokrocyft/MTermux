/*
 * OnePlus 15 Exclusive (ARM64 / Snapdragon 8 Elite Gen 5 / Android 17)
 * 16 KB page-aligned read-only bootstrap archive payload for apt & pkg.
 */
#if !defined(__aarch64__)
# error "OnePlus 15 build supports only aarch64 (arm64-v8a)"
#endif

    .global blob
    .global blob_size
    .hidden blob
    .hidden blob_size

    .section .rodata
    .balign 16384
blob:
    .incbin "bootstrap-aarch64.zip"
1:
    .balign 4
blob_size:
    .int 1b - blob
