Javolution
==========
Javolution is a Java library for real-Time, embedded and high-performance applications.

Developing for BlackBerry?
--------------------------
First of all: I'm sorry. You poor b**tard. I'm sure you felt much the same way I did when you discovered that unlike, say, Android, all you get is a pathetic 1990s API and a toolkit so worthless it makes you want to inflict bodily harm on yourself (not to mention wish for a gigantic fireball to wipe out RIM headquarters).

While it can't make up for some fundamental flaws of the ~~CrapBerry~~ BlackBerry platform, such as the lack of generics support or the complete absence of useful UI APIs, Javolution is a pretty amazing set of libraries that is going to make your life a lot easier (or at least make you a bit less suicidal) with such essentials as a nice and fast Map implementation (can you believe they didn't include Map in their JDK?)

The J2ME version of Javolution is supposed to work on BlackBerry - in theory. In reality, it needs a few tweaks to work correctly. Some small problems prevent it from compiling for J2ME (my guess is nobody noticed) and it needs some tweaks to stop it from conflicting with some built-in BB APIs.

Usage
-----

Our fork of Javolution also includes a pre-compiled and - most importantly - **pre-verified** JAR which is ready to be dropped into your project and will work flawlessly out of the box (with BlackBerry JDK 5.0.0, on other versions your mileage may vary). If you do want to compile from source, clone the repo, go into the `Javolution` directory and run:

```bash
ant clean
ant j2me
```

This will produce (among many other things) a JAR in `target` which you will then have to pre-verify (the `preverify` tool can be obtained with the J2ME SDK straight from ~~Java~~ ~~Sun~~ Oracle).

Note that `ant clean` doesn't really do enough cleaning, if you change stuff and the JAR doesn't reflect that, do an `rm -rf *` in the `target` directory.