## Flash OEM Logo

GUI frontend for a modified load_oemlogo utility for Huawei phones.
Currently tested on U9500 U9200 which use RGB24 oemlogo.mbn files.

###Requirements

  root, USB debugging enabled


###Notes

  The tool is based on a modified Huawei's load_oemlogo executable, which now 
  doesn't
  require the presence of /data/custom.bin file and takes image from 
  /cust/media/oemlogo.mbn.

  The app allows to automatically crop images, resize them to screen dimensions
  and decrease color depth.
  You also can flash oemlogo.mbn file directly if you use advanced color 
  downsampling or dithering.

  Huawei uses raw arrays of BGR24 (24bit) pixels as their oemlogo file format
  for U9500, although the pixels should use colors from 16bit (or even less) 
  RGB color space. 
  Even in this case some images may not display or drive your video-memory 
  crazy. Try differend settings for color downsampling and use with caution.

  If you broke your screen, you may try a [command line flashing utility](https://github.com/fedotawa/flash_oemlogo_u9500).