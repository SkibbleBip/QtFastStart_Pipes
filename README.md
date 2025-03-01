qtfaststart-java
=================

qt-faststart library (and executable) for Java (MIT)

## What is qt-faststart_pipes?

qt-faststart is tiny tool which makes mp4 video file ready for streaming.

Originally designed for reading and writing files, this port implements in-memory processing for datastreams (such as input to/output from FFMPEG wrappers) through the use of InputStream reading and byte array writing. The InputStream is passed to the library and either a byte array containing the output mp4 data is returned or null if the videofile is allready optimized for faststreaming.

It moves 'moov' box, metadata required for starting play, from the end of a file to the beginning.

## Why use this library even there is another one?

I know there is another implementation of qt-faststart for Java in GPL:

https://github.com/LyleE/QTFastStart

This library has below advantages than it:

- MIT licensed.
- Following original code's structure where possible.
- Paying attention to performance; using `FileChannel#transferTo` and not using String comparison.
- Now supports InputStreams and pre-loaded byte arrays of media files!

## Installation

Available from [JCenter](https://bintray.com/bintray/jcenter).

```
repositories {
   jcenter()
}
```

```
compile 'net.ypresto.qtfaststartjava:qtfaststart:0.1.0'
```

## License

MIT license.

## Information about original source code

This product is built upon qt-faststart.c from [FFmpeg](https://github.com/FFmpeg/FFmpeg), which is placed in puglic domain.

Original author of qt-faststart.c: Mike Melanson

Original author of qtfaststart-java: Yuya Tanaka
