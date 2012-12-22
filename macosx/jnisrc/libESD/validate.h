/*
 * validateLib.h
 *   eSellerate SDK 3.6.5
 *   Copyright 2000-2007, Digital River, Inc.
 */

#ifndef _VALIDATE_API_H_
#define _VALIDATE_API_H_

#ifdef __cplusplus
	extern "C" {
#endif

#define eSellerate_ValidateSerialNumber		KNHWXY
#define eWeb_ValidateSerialNumber			KNHWYX
#define eSellerate_Today					KWHNYM
#define eSellerate_StandardizeSerialNumber	KYHNWM
#define eWeb_StandardizeSerialNumber		KYHNMW

typedef unsigned char* eSellerate_String;
typedef short eSellerate_DaysSince2000;


eSellerate_DaysSince2000 eSellerate_ValidateSerialNumber (
  eSellerate_String serialNumber,	/* ASCII Pascal string                   */
  eSellerate_String nameBasedKey,	/* ASCII Pascal string (nil if unneeded) */
  eSellerate_String extraDataKey,	/* ASCII Pascal string (nil if unneeded) */
  eSellerate_String publisherKey	/* ASCII Pascal string (nil if unneeded) */
);

eSellerate_DaysSince2000 eWeb_ValidateSerialNumber (
  const char	*serialNumber,		/* "C" string                   */
  const char	*nameBasedKey,		/* "C" string (nil if unneeded) */
  const char	*extraDataKey,		/* "C" string (nil if unneeded) */
  const char	*publisherKey		/* "C" string (nil if unneeded) */
);
/*
 * return value:
 *   if valid: date (days since January 1 2000) of expiration or (non-expiring) purchase
 *   if invalid: 0
 */


eSellerate_DaysSince2000 eSellerate_Today ( );
/*
 * return value:
 *   days from January 1 2000 to today
 */


void eSellerate_StandardizeSerialNumber (
  eSellerate_String validatedSerialNumber,	/* ASCII Pascal string (source) */
  eSellerate_String correctedSerialNumber	/* ASCII Pascal string (target) */
);

void eWeb_StandardizeSerialNumber (
  const char	*validatedSerialNumber,		/* "C" string (source) */
  char			*correctedSerialNumber		/* "C" string (target) */
);
/*
 * eSellerate_ValidateSerialNumber allows certain tolerances in serialNumber:
 *   lower-case for upper-case, I for 1, O for 0, U for V, and Z for 2.
 * eSellerate_StandardizeSerialNumber takes input string validatedSerialNumber
 * and likewise replaces tolerated characters with corrected characters,
 * but then writes the corrected result to output buffer correctedSerialNumber.
 */

#ifdef __cplusplus
	}
#endif

#endif	// _VALIDATE_API_H_