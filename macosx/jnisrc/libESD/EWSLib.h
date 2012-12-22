/*
 *  EWSLib.h
 *  EWSMac 4.0.0.2
 *
 *  Copyright 2000-2008, Digital River, Inc.
 *
 */

#ifndef _EWEB_API_H_
#define _EWEB_API_H_

#include <Carbon/Carbon.h>

#ifdef __cplusplus
extern "C" {
#endif

/* General Success */
enum {
	E_SUCCESS								= 0			/* The call completed successfully. */
};
	
enum {
// non-error codes --------------------------------
	E_SDK_INSTALLED_ENGINE					= 2001,		/* The latest engine was successfully installed. */
	E_SDK_LATEST_ENGINE_ALREADY_INSTALLED	= 2000,		/* The latest engine is already installed. */
// error codes ------------------------------------
	E_SDK_CREATE_ENTRY_ERROR				= -2001,	/* The SDK failed to add a name/value entnry. */
	E_SDK_BAD_PARAMETER						= -2002,	/* A parameter is incorrect. */
	E_SDK_ENGINE_NOT_INSTALLED				= -2003,	/* There was an error installing the engine. */
	E_SDK_ENGINE_NOT_LOADED					= -2004,	/* There was an error loading the installed engine. */
	E_SDK_ENGINE_CORRUPTED					= -2005,	/* The engine is corrupted. */
	E_SDK_OBJECT_NOT_FOUND					= -2007,	/* The result from the engine could not be found. */
	E_SDK_ENGINE_BUSY						= -2008,	/* The engine is already running within the application. */
	E_SDK_INSTALL_CORRUPTED					= -2009		/* The engine being installed is corrupted. */
};

enum {
// error codes ------------------------------------
	E_INET_CONNECTION_FAILURE				= -3001,	/* A general connection failure has occured. */
	E_INET_ESELLERATE_FAILURE				= -3002,	/* The engine failed to connect to the eSellerate servers. */
	E_INET_DOWNLOAD_ENGINE_FAILURE			= -3003,	/* The engine failed to download an update. */
	E_INET_ESTATE_NOT_FOUND					= -3004		/* The eSellerate engine data is missing from the results. */
};

/* Validate/Manual Activation Specific Return Codes */
enum {
// non-error codes --------------------------------
	E_VALIDATEACTIVATION_MACHINE_MATCH		= 5000,		/* The current machine matches the machine on which the product was activated. */
// error codes ------------------------------------
	E_VALIDATEACTIVATION_MACHINE_MISMATCH	= -5001,	/* The current machine does not match the machine on which the product was activated. */
	E_VALIDATEACTIVATION_OLD_ACTIVATION_KEY	= -5002,	/* The old activation key is invalid. */
	E_ACTIVATION_MANUAL_CANCEL				= -5005		/* The user cancelled a manual activation attempt. */
};

/* Activation Specific Return Codes */
enum {
// error codes ------------------------------------
	E_ACTIVATESN_UNKNOWN_ACTIVATION_KEY		= -25000,	/* The format of the supplied activation key is invalid. */
	E_ACTIVATESN_UNKNOWN_SN					= -25001,	/* An attempt was made to activate an unknown serial number. */
	E_ACTIVATESN_IMPROPER_USAGE				= -25002,	/* Improper usage of the activation routines. */
	E_ACTIVATESN_BLACKLISTED_SN				= -25003,	/* An attempt was made to activate an blacklisted serial number. */
	E_ACTIVATESN_INVALID_ORDER				= -25004,	/* An activation attempt was made on an invalid order. */
	E_ACTIVATESN_LIMIT_MET					= -25005,	/* An activation attempt failed due to the maximum number of allowable activations being met. */
	E_ACTIVATESN_NOT_UNIQUE					= -25009,	/* The serial number to be activated is not unique. */
	E_ACTIVATESN_UNKNOWN_SERVER_ERROR		= -25010,	/* An unknown server error has occured. */
	E_ACTIVATESN_FINALIZATION_ERROR			= -25011,	/* An error occured finalizing the activation on the client machine. */
	E_ACTIVATION_REQUEST_DENIED				= -25012,
	E_ACTIVATESN_NO_SN_FROM_SERVER			= -25013	/* The eSellerate server failed to return a valid activation key. */
};

/* Confirm Serial Number Return codes */
enum {
	// error codes ------------------------------------
	E_CONFIRMATION_NO_SUCH_SERIAL_NUMBER		= -25001,
	E_CONFIRMATION_PUBLISHER_SN_MISMATCH		= -25002,
	E_CONFIRMATION_BLACKLISTED_SERIAL_NUMBER	= -25003,
	E_CONFIRMATION_INVALID_ORDER				= -25004,
	E_CONFIRMATION_UNEXPECTED_FAILURE			= -25010
};

/* Deactivate Serial Number Return Codes */
enum {
	// error codes ------------------------------------
	E_DEACTIVATION_NO_SUCH_SERIAL_NUMBER		= -25001,
	E_DEACTIVATION_BLACKLISTED_SERIAL_NUMBER	= -25003,
	E_DEACTIVATION_INVALID_ORDER				= -25004,
	E_DEACTIVATION_MISSING_ACTIVATION_KEY		= -25006,
	E_DEACTIVATION_MACHINE_MISMATCH				= -25007,
	E_DEACTIVATION_INVALID_ACTIVATION_KEY		= -25008,
	E_DEACTIVATION_NONUNIQUE_SERIAL_NUMBER		= -25009,
	E_DEACTIVATION_NO_SUCH_ACTIVATION_KEY		= -25010,
	E_DEACTIVATION_DEACTIVATION_LIMIT_MET		= -25011
};

/* General Engine Return Codes */
enum {
// non-error codes --------------------------------
	E_ENGINE_SKU_UPDATE_AVAILABLE			= 6000,		/* A SKU update is available from the eSellerate Servers. */
// error codes ------------------------------------
	E_ENGINE_SKU_NO_UPDATE_AVAILABLE		= -6000,	/* No SKU update is available from the eSellerate Servers. */
	E_ENGINE_INTERNAL_ERROR					= -6001,	/* An internal error has occured in the engine. */
	E_ENGINE_PURCHASE_NOTSUCCESSFUL			= -6002		/* A purchase attempt was unsuccessful. */
};

/* System Incompatibility Codes */
enum {
	E_OS_FUNCTIONALITY_UNSUPPORTED			= -7000		/* The system does not meet the minimum requirements for the Embedded Web Store */
};

/*!
    @function	eWeb_IsSystemCompatible
    @abstract   Reports whether the system is compatible with the embedded webstore Engine
    @discussion This function will return true if the system meets the minimum requirements for EWS 
				(i.e. 10.2 or greater with WebKit/Safari installed). This should be called to determine
				if other calls into the engine can be made.
    @result     True if the embedded webstore engine can be loaded on the system, False otherwise.
*/

Boolean eWeb_IsSystemCompatible(void);

/*!
    @function	eWeb_Purchase
    @abstract   Launches the embedded webstore in order to make a purchase.
    @discussion The eWeb_Purchase() function launches the embedded webstore and is a means by
				which a purchase can be made. In order to retrieve the result of the purchase call,
				use the resultdata API described later in this header file.
    @param      eSellerID[in] - eSellerID code.
	@param      skuRefNum[in] - (optional) SKU RefNum.				
	@param      previewID[in] - (optional) Preview Certificate ID Code.				
	@param      layoutCertificate[in] - (optional) Layout Test Certificate.				
	@param      trackingID[in] - (optional) Tracking ID Code.				
	@param      affiliateID[in] - (optional) Affiliate ID Code.				
	@param      couponID[in] - (optional) Coupon ID Code.				
	@param      activationID[in] - (optional) Activation ID Code.				
	@param      extraData[in] - (optional) Extra Data for serial numbers.				
	@param      resultData[out] - Pointer to memory which will be allocated and filled by the Engine upon success.				
    @result     Upon success, it returns E_SUCCESS. If the purchase attempt
				was a failure, the return value is a negative result. For extended error
				information, compare this value against the list of documented eSellerate error codes.
*/

OSStatus eWeb_Purchase(const char *	eSellerID,
					   const char *	skuRefNum,
					   const char *	previewID,
					   const char *	layoutCertificate,
					   const char *	trackingID,
					   const char *	affiliateID,
					   const char *	couponID,
					   const char *	activationID,
					   const char *	extraData,
					   char **	resultData);

/*!
	 @function	 eWeb_PurchaseEx
	 @abstract   Launches the embedded webstore with the extended functionality of prefill.
	 @discussion The eWeb_PurchaseEx() function launches the embedded webstore and is a means by
				 which a purchase can be made. In order to retrieve the result of the purchase call,
				 use the resultdata API described later in this header file. The extended prefill functionality
				 allows you to prefill Embedded Webstore Variables such as cart items, shopper information, and
				 extra data fields.
	 @param      eSellerID[in] - eSellerID code.
	 @param      previewID[in] - (optional) Preview Certificate ID Code.				
	 @param      layoutCertificate[in] - (optional) Layout Test Certificate.				
	 @param      prefillData[in] - Prefill data generated and filled in by the eWeb_NewPrefillData and eWeb_AddPrefillData calls.				
	 @param      resultData[out] - Pointer to memory which will be allocated and filled by the Engine upon success.				
	 @result     Upon success, it returns E_SUCCESS. If the purchase attempt
	 was a failure, the return value is a negative result. For extended error
	 information, compare this value against the list of documented eSellerate error codes.
*/
	
OSStatus eWeb_PurchaseEx(const char *	eSellerID,
						 const char *	previewID,
						 const char *	layoutCertificate,
						 const char *	prefillData,
						 char **		resultData);
	
/*!
	@function	eWeb_CheckForUpdate
	@abstract   Checks for an available update to a given SKU.
	@discussion The eWeb_CheckForUpdate() function launches the embedded webstore and informs
				the user whether there's an update available. If there is, it will allow the 
				user to purchase the udpate.
	@param      eSellerID[in] - eSellerID code.
	@param      skuRefNum[in] - SKU RefNum.				
	@param      previewID[in] - (optional) Preview Certificate ID Code.				
	@param      layoutCertificate[in] - (optional) Layout Test Certificate.				
	@param      trackingID[in] - (optional) Tracking ID Code.				
	@param      affiliateID[in] - (optional) Affiliate ID Code.				
	@param      couponID[in] - (optional) Coupon ID Code.				
	@param      activationID[in] - (optional) Activation ID Code.				
	@param      extraData[in] - (optional) Extra Data for serial numbers.				
	@param      resultData[out] - Pointer to memory which will be allocated and filled by the Engine upon success.				
	@result     Upon success, it returns E_SUCCESS. If the purchase attempt
				was a failure, the return value is a negative result. For extended error
				information, compare this value against the list of documented eSellerate error codes.
 */

OSStatus eWeb_CheckForUpdate(const char *		eSellerID,
							 const char *		skuRefNum,
							 const char *		previewID,
							 const char *		layoutCertificate,
							 const char *		trackingID,
							 const char *		affiliateID,
							 const char *		couponID,
							 const char *		activationID,
							 const char *		extraData,
							 char **	resultData);

/*!
	 @function	 eWeb_CheckForUpdateEx
	 @abstract   Checks for an available update to a given SKU. It extends the eWeb_CheckForUpdate 
				 function by allowing prefill data to be passed.
	 @discussion The eWeb_CheckForUpdateEx() performs the same functionality as eWeb_CheckForUpdate, 
				 with the additional ability of being able to pass in prefill data.
	 @param      eSellerID[in] - eSellerID code.
	 @param      previewID[in] - (optional) Preview Certificate ID Code.				
	 @param      layoutCertificate[in] - (optional) Layout Test Certificate.				
	 @param      prefillData[in] - Prefill data generated and filled in by the eWeb_NewPrefillData and eWeb_AddPrefillData calls.				
	 @param      resultData[out] - Pointer to memory which will be allocated and filled by the Engine upon success.				
	 @result     Upon success, it returns E_SUCCESS. If the purchase attempt
				 was a failure, the return value is a negative result. For extended error
				 information, compare this value against the list of documented eSellerate error codes.
*/
	
OSStatus eWeb_CheckForUpdateEx(const char *	eSellerID,
							   const char *	previewID,
							   const char *	layoutCertificate,
							   const char *	prefillData,
							   char **		resultData);
	
/*!
	@function	eWeb_SilentCheckForUpdate
	@abstract   Checks for an available update to a given SKU.
	@discussion The eWeb_SilentCheckForUpdate function contacts eSellerate servers
				without displaying the Embedded WebStore Engine in order
				to determine if an update is available for the SKU identified by skuRefNum.
	@param      eSellerID[in] - eSellerID code.
	@param      skuRefNum[in] - SKU RefNum.				
	@param      previewID[in] - (optional) Preview Certificate ID Code.				
	@param		askToConnect[in] - Currently not implemented (pass FALSE).
	@param      trackingID[in] - (optional) Tracking ID Code.				
	@result     If an update is available, eWeb_SilentCheckForUpdate returns 
				E_ENGINE_SKU_UPDATE_AVAILABLE. If no update is available it returns 
				E_ENGINE_SKU_NO_UPDATE_AVAILABLE. If the call failed the return value is 
				a negative value. For extended error information compare this result 
				against the list of documented eSellerate error codes.

 */

OSStatus eWeb_SilentCheckForUpdate (const char *	eSellerID,
									const char *	skuRefNum,
									const char *	previewID,
									Boolean			askToConnect,
									const char *	trackingID);

/*!
	@function	eWeb_ConfirmSerialNumber
	@abstract   Confirms that a serial number is valid within the eSellerate system.  
	@discussion This involves confirming that the serial number is known to the eSellerate system, is not blacklisted, 
				and is associated with an order that has not been returned. eWeb_ConfirmSerialNumber 
				works with all supported serial numbers, except those that are manually generated through 
				the Standard serial number interface, or imported to eSellerate for product activation purposes. 
	@param		publisherID[in] - Publisher ID Code.
	@param		serialNumber[in] - Serial Number that has already been activated on the machine.
	@result     E_SUCCESS if the serial number is valid, otherwise returns a negative error code.
 */
OSStatus eWeb_ConfirmSerialNumber (const char	*publisherID,
								   const char	*serialNumber);

/*!
    @function	eWeb_InstallEngineFromPath
    @abstract   Installs a compressed or non-compressed engine to the appropriate location
				on the user's machine.
    @discussion The eWeb_InstallEngineFromPath() function will take a path to either the uncompressed 
				engine framework or the compressed engine and install it to the proper location on the 
				user's machine. This location will either be /Library/Frameworks (if the user has write access)
				or ~/Library/Frameworks.
    @param      path[in] - Path to the compressed (EWSMacCompress.tar.gz) or uncompressed (EWSMac.framework) engine
    @result     If eWeb_InstallEngineFromPath is successful, it returns E_SDK_INSTALLED_ENGINE. If the function fails,
				it will return a negative value. For extended error information, consult the documented list of eSellerate
				error codes.
*/

OSStatus eWeb_InstallEngineFromPath(const char *path);

/*!
    @function	eWeb_NewPrefillData
	@abstract   Allocates a memory region for use by prefill data. 
	@discussion Before passing prefill data to an Embedded Web Store API you must allocate memory for it using eWeb_NewPrefillData.
	@result		A pointer to the prefill data to be passed to eWeb_AddPrefillData(), eWeb_PurchaseEx(), or eWeb_CheckForUpdateEx()
*/

char *eWeb_NewPrefillData(void);

/*!
	@function	eWeb_DisposePrefillData
	@abstract   Disposes the prefill data allocated with eWeb_NewPrefillData().
	@discussion The eWeb_DisposePrefillData will dispose the prefill data allocated by eWeb_NewPrefillData().
	@param      pPrefillData[in] - Pointer allocated by eWeb_NewPrefillData().
 */

void eWeb_DisposePrefillData(char *pPrefillData);

/*!
    @function	eWeb_DisposeResultData
    @abstract   Disposes the result data pointer allocated from an eWeb_* call.
    @discussion The eWeb_DisposeResultData() function will dispose the resultdata pointer
				that is passed into the Purchase and CheckForUpdate calls. This should always
				be called after a Purchase or CheckForUpdate call is made.
    @param      pResultData[in] - Pointer allocated by one of the API calls that accepts a resultData argument.
*/

void eWeb_DisposeResultData(char *pResultData);

/*!
	@function	eWeb_AddPrefillData
	@abstract   Adds prefill data to the internal buffer allocated by eWeb_NewPrefillData. This function 
				must be called once for every name/value pair you wish to add to prefill data.
	@discussion For a listing of all eSellerate variables that Embedded Web Store eSellers can prefill or pull 
				from result data, use the following URL: http://publishers.esellerate.net/embeddedvar.aspx.
	@param      pPrefillData[in] - prefill data buffer returned by eWeb_NewPrefillData.
	@param      pVarName[in] - The name of the eSellerate prefill variable being set.
	@param      pVarValue[in] - The value being given to the eSellerate prefill variable.
 
 */

OSStatus eWeb_AddPrefillData(char *pPrefillData, 
							 const char *pVarName, 
							 const char *pVarValue);

/*!
	@function	eWeb_GetResultDataValueLength
	@abstract   Retrieves the length (in bytes) of a result value given its name. 
	@discussion	eWeb_GetResultDataValueLength() is a utility function for properly 
				allocating memory in which to store result data. Call this API before reading 
				in a result data value so as to prevent buffer overflow.
	@param      pResultData[in] - The result data returned from the Purchase or CheckForUpdate function.
	@param      pVarName[in] - The name for the result data variable for which to search.
	@param      pVarLength[out] - The length of the variables value.
    @result     If the length is successfully retrieved, then eWeb_GetResultDataValueLength()
				returns E_SUCCESS. If the name cannot be found within the result data,
				this function returns E_SDK_OBJECT_NOT_FOUND.
*/

OSStatus eWeb_GetResultDataValueLength(char *pResultData, 
									   const char *pVarName, 
									   unsigned short *pVarLength);

/*!
    @function	eWeb_GetResultDataValue
    @abstract   Retrieves the value of a given result data variable.
    @discussion The eWeb_GetResultDataValue() function retrieves the value of a given 
				result data variable. In order to allocate enough memory for the result data
				variable's value, call eWeb_GetResultDataValueLength().
    @param      pResultData[in] - The result data returned from the Purchase or CheckForUpdate function. 
	@param      pVarName[in] - The name for the result data variable for which to search.
	@param      pVarValue[out] - The memory region in which to hold the result data variable's value.
	@param      pVarValueLen[in] - The size allocated for pVarValue.
    @result     If the value is successfully retrieved, eWeb_GetResultDataValue() returns 
				E_SUCCESS. If the result data variable cannot be found, it returns E_SDK_OBJECT_NOT_FOUND.
*/

OSStatus eWeb_GetResultDataValue(char *pResultData, 
								 char *pVarName, 
								 char *pVarValue, 
								 unsigned short pVarValueLen);

/*!
	@function	eWeb_IndexGetResultDataValue
	@abstract   Retrieves the value for the given record/field combination.
	@discussion The eWeb_IndexGetResultDataValue() function retrieves the value of a given 
				result data field/record combination. In order to allocate enough memory for 
				the result data variable's value, call eWeb_IndexGetResultDataValueLength().
	@param      pResultData[in] - The result data returned from the Purchase or CheckForUpdate function. 
	@param      pRecord[in] - The record name containing the variable being searched.
	@param      pField[in] - The field name within the record you wish to search.
	@param      pWhichIndex[in] - The zero-based index of the name instance.
	@param      pValue[out] - The memory region in which to hold the result data variable's value.
	@param      pValueLen[in] - The size allocated for pValue.
	@result     If the value is successfully retrieved, eWeb_IndexGetResultDataValue() returns 
				E_SUCCESS. If the result data variable cannot be found, it returns E_SDK_OBJECT_NOT_FOUND.
 */

OSStatus eWeb_IndexGetResultDataValue(char *pResultData, 
									  const char *pRecord, 
									  const char *pField, 
									  unsigned short pWhichIndex, 
									  char *pValue, 
									  unsigned short pValueLen);

/*!
	@function	eWeb_IndexGetResultDataValueLength
	@abstract   Retrieves the length for the given record/field combination.
	@discussion The eWeb_IndexGetResultDataValueLength() function retrieves the length (in bytes)
				of a result value given the combination of record and field name and index value.
	@param      pResultData[in] - The result data returned from the Purchase or CheckForUpdate function. 
	@param      pRecord[in] - The record name containing the variable being searched.
	@param      pField[in] - The field name within the record you wish to search.
	@param      pWhichIndex[in] - The zero-based index of the name instance.
	@param      pDataLength[out] - The length (in bytes) of the given record and field.
	@result     If the value length is successfully retrieved, eWeb_IndexGetResultDataValueLength() returns 
				E_SUCCESS. If the result data variable cannot be found, it returns E_SDK_OBJECT_NOT_FOUND.
 */

OSStatus eWeb_IndexGetResultDataValueLength(char *pResultData, 
											const char *pRecord, 
											const char *pField, 
											unsigned short pWhichIndex, 
											unsigned short *pDataLength);

/*!
    @function	eWeb_GetOrderItemByIndex
    @abstract   Returns the order item data at the given zero-based index.
    @discussion The eWeb_GetOrderItemByIndex() function returns the order
				item data at the given zero-based index. Each output buffer must be 
				either NULL (in which case that value will be ignored), or at least
				256 bytes in length. Values greater than 256 will be truncated.
				Buffers that are smaller than 256 bytes in length will cause undefined behavior.
	@param      pResultData[in] - The result data returned from the Purchase or CheckForUpdate function. 
	@param      pWhichIndex[in] - The zero-based index of the name instance.
	@param      pSkuRefNum[out] - (optional) The original SKU purchased.
	@param      pRedirectSkuRefNum[out] - (optional) The SKU RefNum that was actually purchased in the case of a SKU redirect.
	@param      pQuantity[out] - (optional) The quantity of the item being purchased.
	@param      pRegistrationName[out] - (optional) The name on which the serial number was based.
	@param      pSerialNumber[out] - (optional) The serial number associated with this purchase.
	@param      pPromptedValue[out] - (optional) The prompted value entered by the user given one was requested.
	@param      pActivationID[out] - (optional) The activation ID for the product given that an activation was requested.
	@param      pActivationKey[out] - (optional) The activation key for the product given than an activation was requested.
	@param      pDownloadURL[out] - (optional) The eSellerate URL from which this product may be downloaded (only valid for a period of time).
    @result     True if a value is found, False if no entry exists for that index.
*/

Boolean eWeb_GetOrderItemByIndex(char *pResultData,
							  unsigned short pWhichIndex,
							  char *pSkuRefNum,
							  char *pRedirectSkuRefNum,
							  char *pQuantity,
							  char *pRegistrationName,
							  char *pSerialNumber,
							  char *pPromptedValue,
							  char *pActivationID,
							  char *pActivationKey,
							  char *pDownloadURL);

/*!
	@function	eWeb_ActivateSerialNumber
	@abstract   Activates a serial number.
	@discussion The eWeb_ActivateSerialNumber function activates a serial number for use on the current
				host machine using eSellerate Product Activation.
	@param		publisherID[in] - Publisher ID Code.
	@param		activationID[in] - Activation ID Code, for Serial Number Activation.
	@param		serialNumber[in] - Serial Number to activate.
	@param		askToConnect[in] - Displays the manual activation wizard if no connection is available.
	@result     If the serial number is properly activated, E_SUCCESS is returned. The following
				error values are activation specific.
 
				E_ACTIVATESN_UNKNOWN_SERVER_ERROR - An unknown error occured by the eSellerate servers during activation.
				E_ACTIVATESN_UNKNOWN_ACTIVATION_KEY - An unknown activation key was used.
				E_ACTIVATESN_UNKNOWN_SN - An unknown serial number was used.
				E_ACTIVATESN_IMPROPER_USAGE - Product activation was used improperly.
				E_ACTIVATESN_BLACKLISTED_SN - Activation failed because the serial number is blacklisted.
				E_ACTIVATESN_INVALID_ORDER - The serial number's corresponding order is invalid.
				E_ACTIVATESN_LIMIT_MET - No more activations are allowed on this serial number.
				E_ACTIVATESN_NOT_UNIQUE - Activation failed because the serial number is not unique.
				E_ACTIVATESN_FINALIZATION_ERROR - The activation routines couldn't complete the activation on the host machine.
 */

OSStatus eWeb_ActivateSerialNumber(const char *publisherID,
								   const char *activationID,
								   const char *serialNumber,
								   Boolean	  askToConnect);

/*!
	@function	eWeb_DeactivateSerialNumber
	@abstract   Deactivates a serial number that has been activated on the user's machine. 
	@discussion The eWeb_DeactivateSerialNumber call connects to our servers and deactivates an already-activated machine
			    in our records. It also removes the activation from the user's machine so that subsequent calls to 
				eWeb_ValidateActivation will fail.
	@param		publisherID[in] - Publisher ID Code.
	@param		activationID[in] - Activation ID Code.
	@param		serialNumber[in] - Serial Number that has already been activated on the machine.
	@result     If the serial number has been activated on the user's system and the deactivation count has not been exceeded
				for that serial number, the activation will be removed from the server and from the user's machine.
 */

OSStatus eWeb_DeactivateSerialNumber(const char *publisherID,
									 const char *activationID,
									 const char *serialNumber);

/*!
	@function	eWeb_ValidateActivation
	@abstract   Determines whether a serial number was activated on the host machine.
	@discussion The eWeb_ValidateActivation function determines whether a supplied serial number has
				been activated on the host machine using eSellerate Product Activation.
	@param		publisherID[in] - Publisher ID Code.
	@param		activationID[in] - Activation ID Code, for Serial Number Activation.
	@param		serialNumber[in] - Serial Number to activate.
	@result     If the serial number was successfully activated on the user's machine, eWeb_ValidateActivation()
				return E_VALIDATEACTIVATION_MACHINE_MATCH. Otherwise, the API returns 
				E_VALIDATEACTIVATION_MACHINE_MISMATCH. If the function call fails, the return valus is a negative
				value. For extended error information, consult the documented list of eSellerate error codes.
 */

OSStatus eWeb_ValidateActivation(const char *publisherID,
								 const char *activationID,
								 const char *serialNumber);

/*!
	@function	eWeb_DateActivation
	@abstract   Returns the date an activation was performed on the machine (in days since January 1, 2000). 
	@discussion The eWeb_DateActivation returns the date that an activation was actually performed on the user's system.
	@param		publisherID[in] - Publisher ID Code.
	@param		activationID[in] - Activation ID Code, for Serial Number Activation.
	@param		serialNumber[in] - Serial Number that has already been activated.
	@result     If the serial number was already activated on the user's system, this will return a value greater than zero
				representing the number of days since January 1, 2000 that the serial number was activated.
*/
unsigned int eWeb_DateActivation (const char *publisherID,
								   const char *activationID,
								   const char *serialNumber);

/*!
	@function	eWeb_ManualActivateSerialNumber
	@abstract   Launches the Embedded Webstore Engine to allow clients to manually activate a serial number.
	@discussion The eWeb_ManualActivateSerialNumber function launches the Embedded Webstore Engine to allow 
				clients to manually activate a serial number. Typically this API is used when the current machine
				cannot get a network connection but yet a serial number needs to be activated.
	@param		publisherID[in] - Publisher ID Code.
	@param		activationID[in] - Activation ID Code, for Serial Number Activation.
	@param		serialNumber[in] - Serial Number to activate.
	@param		titlePreface[in] - (optional)Custom Text to Preface Wizard Titles.
	@param		otherOptions[in] - (optional)Custom Text to include Phone Number, Email Address, or other contact option.
	@result     If user cancels durin gthe processing of manual activation, eWeb_ManualActivateSerialNumber returns
				E_ACTIVATION_MANUAL_CANCEL. If the serial number is successfully activated, the return value is
				E_SUCCESS. In the event of a failure, see the documnted eSellerate error codes.
 */
OSStatus eWeb_ManualActivateSerialNumber(const char *publisherID,
										 const char *activationID,
										 const char *serialNumber,
										 const char *titlePreface,
										 const char *otherOptions);


/*!
	@function	eWeb_Today
	@abstract   Returns the number of days since January 1 2000.
	@discussion This function can be used to validate serial number or activation expirations.
	@result     Number of days since January 1, 2000.
 */
unsigned int eWeb_Today();

#ifdef __cplusplus
}
#endif

#endif
