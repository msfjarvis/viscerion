/* SPDX-License-Identifier: BSD
 *
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 *
 */

#if defined(__ANDROID_API__) && __ANDROID_API__ < 24
char *strchrnul(const char *s, int c);
#endif

