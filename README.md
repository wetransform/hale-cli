hale-cli
========

[![Docker Hub Badge](https://img.shields.io/badge/Docker-Hub%20Hosted-blue.svg)](https://hub.docker.com/r/wetransform/hale-cli/)

hale command line interface.
Lists available commands when run without arguments.

From version 5 onwards, hale-cli versions don't necessarily resemble the version of the respective [hale»studio](https://github.com/halestudio/hale) dependencies used.
Here you can find an overview on which hale»studio version is used:

| hale-cli | hale»studio (major version) | Java
| -------- | --------------------------- | ----
| 5.x      | 5                           | 17
| 4.x      | 4                           | 8
| 3.x      | 3                           | 8


Build
-----

Build distribution archive:

```
./gradlew distZip
```

Build Debian package:

```
./gradlew buildDeb
```

Build docker image:

```
./gradlew dockerBuildImage
```


Run
---

You can run the application using Gradle.

```
./gradlew run
```

Alternatively, you can run the start script of the built application.

```
hale --version
```

You can provide arguments to run as string in `""` following the below syntax.
For example pass argument `help` to run command as below
```
./gradlew run --args="help"
```

If using `./gradlew installDist`, the start script can be found in `./build/install/hale/bin/`.

JVM parameters can be provided to the start script with the `HALE_OPTS` environment variable.


Use custom hale studio dependencies during development
------------------------------------------------------

If you do changes to hale studio and want to test your changes with hale-cli in your local environment, then you need to perform these steps:

1.  Build hale studio locally and publish the related artifacts to you local Maven repository.

    For that change to the `build/` folder in your hale studio checkout and run the following command:

    ```
    ./build.sh clean && ./build.sh installArtifacts`
    ```
2.  Make sure that in the `build.gradle` file the line adding the `mavenLocal` repository is uncommented (see [here](https://github.com/halestudio/hale-cli/blob/1ac56a52c359e52d71fe210b1cc4681aa53e3edb/build.gradle#L40)).
3.  If necessary adapt the version of the hale-studio dependencies to the ones you published (see [here](https://github.com/halestudio/hale-cli/blob/1ac56a52c359e52d71fe210b1cc4681aa53e3edb/build.gradle#L29)).

Gradle will then take care to use the local dependencies.
If you want to create a local build of hale-cli that you want to use, you can for instance run

```
./gradlew clean installDist
```


Configuration
-------------


### Logging

The system properties `log.hale.level` and `log.root.level` can be set to control the default logging levels.

```
HALE_OPTS="-Dlog.hale.level=INFO -Dlog.root.level=WARN"
```


### Proxy connection

If you need to connect to the internet via a proxy server, you need to provide that information as system properties as well.

The following system properties can be provided to configure the proxy:

* `http.proxyHost` - the proxy host name or IP address
* `http.proxyPort` - the proxy port number
* `http.nonProxyHosts` - hosts for which the proxy should not be used, separated by | (optional)
* `http.proxyUser` - user name for authentication with the proxy (optional)
* `http.proxyPassword` - password for authentication with the proxy (optional)

Example:

```
HALE_OPTS="-Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=8080 -Dhttp.nonProxyHosts='localhost|host.example.com'"
```


### Language

Some commands may produce different results based on your language.
By default the system language is used.
You can override the default locale settings via the following system properties:

* `user.language` - two letter code for the language (e.g. `de`)
* `user.country` - two letter code for the country (e.g. `DE`)
* `user.variant` - name of the variant, if applicable

Example:

```
HALE_OPTS="-Duser.country=DE -Duser.language=de"
```


Helpers
-------

Check which files were installed by the `.deb` package:

```
dpkg-query -L hale-cli
```
