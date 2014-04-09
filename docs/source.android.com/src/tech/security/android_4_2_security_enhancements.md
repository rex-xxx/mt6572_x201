#Security Enhancements in Android 4.2

Android provides a multi-layered security model described in the [Android
Security Overview](index.html).  Each update to Android includes dozens of
security enhancements to protect users.  The following are some of the security
enhancements introduced in Android 4.2:

+ **Application verification.**  Users can choose to enable “Verify Apps" and
have applications screened by an application verifier, prior to installation.
App verification can alert the user if they try to install an app that might be
harmful; if an application is especially bad, it can block installation.

+ **More control of premium SMS.** Android will provide a notification if an
application attempts to send SMS to a short code that uses premium services
which might cause additional charges.  The user can choose whether to allow the
application to send the message or block it.

+ **Always-on VPN.**  VPN can be configured so that applications will not have
access to the network until a VPN connection is established.  This prevents
applications from sending data across other networks.

+ **Certificate Pinning.** The Android core libraries now support
[certificate pinning](https://developer.android.com/reference/android/net/http/X509TrustManagerExtensions.html). Pinned domains will receive a certificate validation
failure if the certificate does not chain to a set of expected certificates.
This protects against possible compromise of Certificate Authorities.

+ **Improved display of Android permissions.** Permissions have been organized
into groups that are more easily understood by users.  During review of the
permissions, the user can click on the permission to see more detailed
information about the permission.

+ **installd hardening.** The installd daemon does not run as the root user,
reducing potential attack surface for root privilege escalation.

+ **init script hardening.**  init scripts now apply O_NOFOLLOW semantics to
prevent symlink related attacks.

+ **FORTIFY_SOURCE.**  Android now implements FORTIFY_SOURCE. This is used by
system libraries and applications to prevent memory corruption.

+ **ContentProvider default configuration.**  Applications which target API
level 17 will have "export" set to "false" by default for each
[ContentProvider](https://developer.android.com/reference/android/content/ContentProvider.html),
reducing default attack surface for applications.

+ **Cryptography.** Modified the default implementations of SecureRandom and
Cipher.RSA to use OpenSSL.  Added SSL Socket support for TLSv1.1 and TLSv1.2
using OpenSSL 1.0.1

+ **Security Fixes.** Upgraded open source libraries with security fixes include
WebKit, libpng, OpenSSL, and LibXML. Android 4.2 also includes fixes for
Android-specific vulnerabilities. Information about these vulnerabilities has
been provided to Open Handset Alliance members and fixes are available in
Android Open Source Project.  To improve security, some devices with earlier
versions of Android may also include these fixes.
