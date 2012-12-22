/*
 * validate.h
 *   Copyright 2000-2003, eSellerate Inc.
 *   All rights reserved worldwide.
 */

#ifndef _VALIDATE_API_H_
#define _VALIDATE_API_H_

#ifdef __cplusplus
  extern "C" {
#endif

typedef char* eSellerate_String;
typedef short eSellerate_DaysSince2000;

eSellerate_DaysSince2000 __stdcall
eSellerate_ValidateSerialNumber (
  eSellerate_String serialNumber, /* 0-terminated ASCII string                    */
  eSellerate_String nameBasedKey, /* 0-terminated ASCII string (NULL if unneeded) */
  eSellerate_String extraDataKey, /* 0-terminated ASCII string (NULL if unneeded) */
  eSellerate_String publisherKey  /* 0-terminated ASCII string (NULL if unneeded) */
);
/*
 * return codes:
 *   if valid: date (days since January 1 2000) of expiration or (non-expiring) purchase
 *   if invalid: 0
 */

eSellerate_DaysSince2000 __stdcall
eSellerate_Today ( ); /* days from January 1 2000 to today */

#ifdef __cplusplus
  }
#endif

#endif
