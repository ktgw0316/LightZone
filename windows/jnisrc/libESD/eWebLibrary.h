/*
 * eWebLibrary.h
 *   Copyright 2000-2005, eSellerate Inc.
 *   All rights reserved worldwide.
 */

#ifndef _EWEB_API_H_
#define _EWEB_API_H_

#include <windows.h>

#ifdef __cplusplus
extern "C" {
#endif


////////////////////////////////////////////////eSellerate Return Values////////////////////////////////////////////////
/* PLEASE READ: 
 *      The following are eSellerate-Specific return values and their descriptions. The return values
 * are divided into five sections - engine, activation, operating system, sdk, and networking. Each 
 * return value is a valid Win32 HRESULT. Our return values do not conflict with Windows System or Networking (WinINET)
 * return values, but complement them. As a result, valid Windows System or Networking error values may be returned
 * from the eSellerate Embedded WebStore API in the event that they do not fall into any of the categories defined below. 
 * See the documentation for each eSellerate function to determine valid success values, and potential error values. 
 * In order to determine whether the return value was a generic success, utilize the Win32 macro SUCCEEDED(). In order
 * to determine if the return value was a generic failure, utilize the Win32 macro FAILED(). To determine that the
 * return value is an eSellerate return value as opposed to a Windows return value, use the eSellerate macro
 * IS_ESELLRETURNCODE().
 *
 *
 * Ex:
 *              BYTE* bypResultData; 
 *              HRESULT hr = eWeb_Purchase(
 *                     "STRXXXXXXXXX",
 *                     "SKUXXXXXXXXX",
 *                     "","","","","","","",
 *                     &bypResultData);
 *              if(SUCCEEDED(hr))
 *              {
 *                ... purchase successfull logic ...
 *              }
 *              else if(FAILED(hr) && IS_ESELLRETURNCODE(hr))
 *              {
 *                ... handle various esellerate errors ...
 *              }
 *              else
 *              {
 *                ... handle windows/wininet error code ...
 *              }
 *           
 *      Under most circumstances, Windows error codes are not returned barring an exceptional error (permissions errors, 
 * network failure, etc.). For example, if the user quits out of a purchase, E_ENGINE_PURCHASE_NOTSUCCESSFUL is returned
 * as opposed to a windows error. It is recommended to keep in mind that these API return Windows error codes for 
 * debugging and informational purposes, but under normal conditions Windows errors will not be used. 
 */
#define IS_ESELLRETURNCODE(x) \
	((((DWORD)x)&0x20000000)?1:0)

     /* General Success */
#define E_SUCCESS                               0x00000000 /* The call completed successfully. */ 
     /* General Engine Return Codes */
#define E_ENGINE_BAD_SDK_INPUT                  0xe0000000 /* A parameter is incorrect. */
#define E_ENGINE_SKU_UPDATE_AVAILABLE           0x60000006 /* A SKU update is available from the eSellerate Servers. */
#define E_ENGINE_SKU_NO_UPDATE_AVAILABLE        0x60000007 /* No SKU update is available from the eSellerate Servers. */
#define E_ENGINE_INTERNAL_ERROR                 0xe0000003 /* An internal error has occured in the engine. */
#define E_ENGINE_PURCHASE_NOTSUCCESSFUL         0xe0000004 /* A purchase attempt was unsuccessful. */ 
     /* Activation Specific Return Codes */
#define E_VALIDATEACTIVATION_MACHINE_MISMATCH   0xe0050000 /* The current machine does not match the machine on which the product was activated. */
#define E_VALIDATEACTIVATION_MACHINE_MATCH      0x20050001 /* The current machine matches the machine on which the product was activated. */
#define E_ACTIVATION_INVALID_ACTIVATION_KEY     0xe0050001 /* The supplied activation key is invalid. */
#define E_ACTIVATION_UNKNOWN_ACTIVATION_KEY     0xe0050002 /* The format of the supplied activation key is invalid. */
#define E_ACTIVATESN_UNKNOWN_SERVER_ERROR       0xe0050003 /* An unknown server error has occured. */
#define E_ACTIVATESN_UNKNOWN_SN                 0xe0050004 /* An attempt was made to activate an unknown serial number. */
#define E_ACTIVATESN_IMPROPER_USAGE             0xe0050005 /* Improper usage of the activation routines. */
#define E_ACTIVATESN_BLACKLISTED_SN             0xe0050006 /* An attempt was made to activate a blacklisted serial number. */
#define E_ACTIVATESN_INVALID_ORDER              0xe0050007 /* An activation attempt was made on an invalid order. */
#define E_ACTIVATESN_LIMIT_MET                  0xe0050008 /* An activation attempt failed due to the maximum number of allowable activations being met. */
#define E_ACTIVATESN_NOT_UNIQUE                 0xe0050009 /* The serial number to be activated is not unique. */
#define E_ACTIVATESN_FINALIZATION_ERROR         0xe005000a /* An error occured finalizing the activation on the client machine. */
#define E_ACTIVATESN_NO_SN_FROM_SERVER          0xe005000b /* The eSellerate server failed to return a valid activation key. */
#define E_ACTIVATION_MANUAL_CANCEL              0xe005000c /* The user cancelled a manual activation attempt. */
     /* OS Support Return Values. */
#define E_OS_FUNCTIONALITY_UNSUPPORTED          0xe0030000 /* The engine requires functionality of the operating system that is not available. */
     /* SDK Return Values. */
#define E_SDK_LATEST_ENGINE_ALREADY_INSTALLED   0x20010000 /* The latest engine is already installed. */
#define E_SDK_INSTALLED_ENGINE                  0x20010001 /* The latest engine was successfully installed. */
#define E_SDK_ERROR_REGISTERING_ENGINE          0xe0010003 /* The latest engine failed to register. */
#define E_SDK_ENGINE_NOT_INSTALLED              0xe0010004 /* No engine is not installed. */
#define E_SDK_CREATE_ENTRY_ERROR                0xe0010005 /* The SDK failed to add a name/value entry. */
#define E_SDK_ENGINE_CORRUPTED                  0xe0010006 /* The engine is corrupted. */
#define E_SDK_ENGINE_RES_ERR                    0xe0010007 /* The engine resource could not be used. */
#define E_SDK_BAD_PARAMETER                     0xe0010008 /* A parameter is incorrect. */
#define E_SDK_OBJECT_NOT_FOUND                  0xe0010009 /* The SDK could not find a requsted data object or value. */
     /* Network Specific Return Codes. */
#define E_INET_CONNECTION_FAILURE               0xe0020000 /* A general connection failure has occured. */
#define E_INET_SILENT_CONNECTION_FAILURE        0xe0020002 /* The engine could not reach the network because it is operating in silent mode. */
#define E_INET_DEVICE_CONNECTION_FAILURE        0xe0020003 /* The engine failed to reach the network due to a device failure. */
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////eSellerate API///////////////////////////////////////////////////////
/* PLEASE READ: 
 * The following is the complete set of eSellerate Embedded WebStore functions. See each individual function for
 * a description. All parameters are required unless otherwise stated. In the event an improper parameter is
 * passed, all API return E_SDK_BAD_PARAMETER. 
 */

/* NAME:    eWeb_IsSystemCompatible()
 * 
 * DESCRIPTION: The eWeb_IsSystemCompatible() functions determines whether the operating
 *          system will support the Embedded WebStore Engine. If the event that the 
 *          function determines the system is incompatible, one can switch to using
 *          the eSellerateEngine by including it in your product. 
 *
 * RETURNS: TRUE if the system is compatible, or FALSE if the system is not compatible. 
 *          System requirements are Microsoft Internet Explorer 5.5 or higher.
 * 
 */
BOOL _stdcall eWeb_IsSystemCompatible(VOID);

/* NAME:    eWeb_Purchase()
 * 
 * DESCRIPTION: The eWeb_Purchase function launches the embedded webstore and is a means by
 *          which a purchase can be made. In order to retrive the result of the purchase call,
 *          use the resultdata API described later in this header file. 
 *
 * RETURNS: Upon success, it returns E_SUCCESS. If the purchase
 *          attempt was a failure, the return value is a negative HRESULT. For extended
 *          error information, compare this value against the list of documented eSellerate 
 *			or Windows error codes. 
 * 
 */
HRESULT __stdcall eWeb_Purchase (            
    LPCSTR szEsellerID,                       // [in] eSeller ID code.
    LPCSTR szSkuRefNum,                       // [in] SKU RefNum (optional).
    LPCSTR szPreviewID,                       // [in] Preview Certificate ID Code (optional).
    LPCSTR szLayoutCertificate,               // [in] Layout Test Certificate (optional).
    LPCSTR szTrackingID,                      // [in] Tracking ID Code (optional).
    LPCSTR szAffiliateID,                     // [in] Affiliate ID Code (optional).
    LPCSTR szCouponID,                        // [in] Coupon ID Code (optional).
    LPCSTR szActivationID,                    // [in] Activation ID Code, for Serial Number Activation. (optional).
    LPCSTR szExtraData,                       // [in] Extra Data (optional).
    BYTE** byppResultData                     // [out] Pointer to memory which will be allocated and filled by the Engine upon success.
);

/* NAME:    eWeb_PurchaseEx()
 * 
 * DESCRIPTION: The eWeb_PurchaseEx() function extends the eWeb_Purchase() function 
 *          by allowing developers to supply prefill information. This prefill information
 *          is consumed by eSellerate stores and allows added features for webstore 
 *          usability. In order to retrieve the result of the purchase call, use 
 *          the resultdata API described later in this header file. In order to add
 *          to the prefill data, utilize the eWeb_NewPrefillData() and eWeb_AddPrefillData()
 *          API. 
 *
 * RETURNS: Upon success, it returns E_SUCCESS. If the purchase
 *          attempt was a failure, the return value is a negative HRESULT. For extended
 *          error information, compare this value against the list of documented eSellerate 
 *			or Windows error codes.
 * 
 */
HRESULT __stdcall eWeb_PurchaseEx (           
    LPCSTR szEsellerID,                       // [in] eSeller ID code.
    LPCSTR szPreviewID,                       // [in] Preview Certificate ID Code (optional).
    LPCSTR szLayoutCertificate,               // [in] Layout Test Certificate (optional).
    BYTE*  bypPrefillData,                    // [in] Prefill Data.
    BYTE** byppResultData                     // [out] Pointer to memory which will be allocated and filled by the Engine upon success.
);

/* NAME:    eWeb_CheckForUpdate()
 * 
 * DESCRIPTION: The eWeb_CheckForUpdate function checks for an available update to
 *          a given SKU. 
 *
 * RETURNS: Upon success, it returns E_SUCCESS. If the purchase of the update
 *          was a failure, the return value is a negative HRESULT. For extended
 *          error information, compare this value against the list of documented eSellerate 
 *			or Windows error codes.
 * 
 */
HRESULT __stdcall eWeb_CheckForUpdate ( 
    LPCSTR szEsellerID,                       // [in] eSeller ID Code.
    LPCSTR szSkuRefNum,	                      // [in] SKU RefNum.
    LPCSTR szPreviewID,                       // [in] Preview Certificate ID Code (optional).
    LPCSTR szLayoutCertificate,	              // [in] Layout Test Certificate (optional).
    LPCSTR szTrackingID,                      // [in] Tracking ID Code (optional). 
    LPCSTR szAffiliateID,                     // [in] Affiliate ID Code (optional). 
    LPCSTR szCouponID,                        // [in] Coupon ID Code (optional).
    LPCSTR szActivationID,                    // [in] Activation ID Code, to Activate a Serial Number (optional).
    LPCSTR szExtraData,                       // [in] Extra Data (optional). 
    BYTE** byppResultData                     // [out] Pointer to memory which will be allocated and filled by the Engine upon success.
);

/* NAME:    eWeb_CheckForUpdateEx()
 * 
 * DESCRIPTION: The eWeb_CheckForUpdateEx function checks for an available update to
 *          a given SKU. It extends the eWeb_CheckForUpdate() function by allowing
 *          prefill data to be passed. 
 *
 * RETURNS: Upon success, it returns E_SUCCESS. If the purchase of the update
 *          was a failure, the return value is a negative HRESULT. For extended
 *          error information, compare this value against the list of documented eSellerate 
 *			or Windows error codes.
 * 
 */
HRESULT __stdcall eWeb_CheckForUpdateEx (
    LPCSTR szEsellerID,                       // [in] eSeller ID code.
    LPCSTR szPreviewID,                       // [in] Preview Certificate ID Code (optional).
    LPCSTR szLayoutCertificate,               // [in] Layout Test Certificate (optional).
    BYTE*  bypPrefillData,                    // [in] Prefill Data.
    BYTE** byppResultData                     // [out] Pointer to memory which will be allocated and filled by the Engine upon success.
);

/* NAME:    eWeb_SilentCheckForUpdate()
 * 
 * DESCRIPTION: The eWeb_SilentCheckForUpdate function contacts eSellerate
 *          servers without displaying the Embedded WebStore Engine in order
 *          to determine if an update is available for the SKU identified by
 *          skuRefNum. 
 *
 * RETURNS: If an update is available, eWeb_SilentCheckForUpdate returns 
 *          E_ENGINE_SKU_UPDATE_AVAILABLE. If no update is available 
 *          it returns E_ENGINE_SKU_NO_UPDATE_AVAILABLE. If the call failed
 *			the return value is a negative HRESULT. For extended error information
 *			compare this result against the list of documented eSellerate or Windows
 *			error codes. 
 * 
 */
HRESULT __stdcall eWeb_SilentCheckForUpdate (
    LPCSTR szEsellerID,                       // [in] eSeller ID code.
    LPCSTR szSkuRefNum,                       // [in] SKU RefNum.
    LPCSTR szPreviewID,                       // [in] Preview Certificate ID Code (optional).
    BOOL   bAskToConnect,                     // [in] Ask to connect when no active connection is found.
    LPCSTR szTrackingID                       // [in] Tracking ID Code (optional).
);

/* NAME:    eWeb_ActivateSerialNumber()
 * 
 * DESCRIPTION: The eWeb_ActivateSerialNumber function activates a serial number
 *          for use on the current host machine using eSellerate Product Activation.
 *
 * RETURNS: If the serial number is properly activated, E_SUCCESS is returned. The following
 *         errors codes are activation specific. Each error code is shown its corresponding
 *         value in the eSellerateEngine.dll API for migration from legacy applications
 *         as well as a short description of the conditions under which each might occur. The value
 *         in the parenthesis does not reflect the value of the error code under the new SDK, but the 
 *         value of the error code under the old SDK and exists for migration purposes only. 
 *        
 *         E_ACTIVATESN_UNKNOWN_SERVER_ERROR (N/A) - An unknown error occured by the eSellerate servers during activation. 
 *         E_ACTIVATION_UNKNOWN_ACTIVATION_KEY (-25000)  - An unknown activation key was used. 
 *         E_ACTIVATESN_UNKNOWN_SN (-25001) - An unknown serial number was used. 
 *         E_ACTIVATESN_IMPROPER_USAGE (-25002) - Product activation is used improperly. 
 *         E_ACTIVATESN_BLACKLISTED_SN (-25003) - Activation failed because the serial number is blacklisted. 
 *         E_ACTIVATESN_INVALID_ORDER (-25004) - The serial number's corresponding order is invalid. 
 *         E_ACTIVATESN_LIMIT_MET (-25005) - No more activations are allowed on this serial number. 
 *         E_ACTIVATESN_NOT_UNIQUE (-25009) - Activation failed because the serial number is not unique. 
 *         E_ACTIVATESN_FINALIZATION_ERROR (N/A) - The activation routines couldn't complete the activation on the host machine. 
 * 
 */
HRESULT __stdcall eWeb_ActivateSerialNumber (
    LPCSTR szPublisherID,                     // [in] Publisher ID Code.
    LPCSTR szActivationID,                    // [in] Activation ID Code, for Serial Number Activation.
    LPCSTR szSerialNumber,                    // [in] Serial Number to Activate.
    BOOL   bAskToConnect                      // [in] Ask to Connect when no Active Connection is Found.
);

/* NAME:    eWeb_ValidateActivation()
 * 
 * DESCRIPTION: The eWeb_ValidateActivation function determines whether
 *          a supplied serial number has been activated on the host machine
 *          using eSellerate Product Activation.
 *
 * RETURNS: If the serial number was successfully activated on the 
 *          user's machine, eWeb_ValidateActivation() returns 
 *          E_VALIDATEACTIVATION_MACHINE_MATCH. Otherwise, the API
 *          returns E_VALIDATEACTIVATION_MACHINE_MISMATCH. If the function
 *			call fails, the return value is a negative HRESULT. For extended
 *			error information, consult the documented list of eSellerate or
 *			Windows error codes. 
 * 
 */
HRESULT __stdcall eWeb_ValidateActivation (
    LPCSTR szPublisherID,                     // [in] Publisher ID Code.
    LPCSTR szActivationID,                    // [in] Activation ID Code, for Serial Number Activation Validation.
    LPCSTR szSerialNumber                     // [in] Serial Number.
);

/* NAME:    eWeb_ManualActivateSerialNumber()
 * 
 * DESCRIPTION: The eWeb_ManualActivateSerialNumber function launches the
 *          Embedded WebStore Engine to allow clients to manually activate
 *          a serial number. Typically this API is used when the current 
 *          machine cannot get a network connection but yet a serial number
 *          needs to be activated. 
 *
 * RETURNS: If the user cancels during the processing of manual activation, 
 *          eWeb_ManualActivateSerialNumber returns E_ACTIVATION_MANUAL_CANCEL. 
 *          If the serial number is successfully activated, the return value 
 *          is E_SUCCESS. In the event of a failure, see the documented eSellerate and
 *          Windows error codes. 
 * 
 */
HRESULT __stdcall eWeb_ManualActivateSerialNumber (
    LPCSTR szPublisherID,                     // [in] Publisher ID Code.
    LPCSTR szActivationID,                    // [in] Activation ID Code, for Serial Number Activation.
    LPCSTR szSerialNumber,                    // [in] Serial Number.
    LPCSTR szTitlePreface,                    // [in] Custom Text to Preface Wizard Titles (optional). 
    LPCSTR szOtherOptions                     // [in] Custom Text to include Phone Number, Email Address or other Contact (optional). 
);

/* NAME:    eWeb_InstallEngineFromResource()
 * 
 * DESCRIPTION: The eWeb_InstallEngineFromResource function installs
 *          an eSellerate Embedded WebStore Engine onto the user's machine from
 *          a resource embedded within a module file. 
 *
 * RETURNS: If eWeb_InstallEngineFromResource is successful, it returns E_SDK_INSTALLED_ENGINE. 
 *          If the latest engine is already installed on the user's machine, it returns
 *          E_SDK_LATEST_ENGINE_ALREADY_INSTALLED. Both of these are positive HRESULTs and 
 *          indicate success. If this function fails, the return value is a negative HRESULT. 
 *          For extended error information, consult the documented list of eSellerate or Windows error
 *          codes. 
 * 
 */
HRESULT __stdcall eWeb_InstallEngineFromResource(
    HINSTANCE hOwnerModule,                   // [in] Handle to a valid Win32 module that contains the engine resource.
    INT       iResourceID,                    // [in] The ID given to the engine resource within 'hOwnerModule'. 
    LPCSTR    szResourceType                  // [in] The Type-name given to the engine resource within 'hOwnerModule'. 
);

/* NAME:    eWeb_InstallEngineFromPath()
 * 
 * DESCRIPTION: The eWeb_InstallEngineFromPath function installs a non-compressed engine
 *          onto the user's machine. Compressed engines are not valid for this call. 
 *
 * RETURNS: If eWeb_InstallEngineFromPath() is successful, it returns E_SDK_INSTALLED_ENGINE. 
 *          If the latest engine is already installed on the user's machine, it returns
 *          E_SDK_LATEST_ENGINE_ALREADY_INSTALLED. Both of these are postive HRESULTs and 
 *          indicate success. If this function fails, the return value is a negative HRESULT. 
 *          For extended error information, consult the documented list of eSellerate or Windows error
 *          codes. 
 * 
 */
HRESULT __stdcall eWeb_InstallEngineFromPath(
    LPCSTR szPath                             // [in] A path string that locates the engine to install. 
);

/* NAME:    eWeb_ApplicationLocation()
 * 
 * DESCRIPTION: The eWeb_ApplicationLocation function returns the path to the application module
 *          calling the Embedded WebStore Engine.
 *
 * RETURNS: If eWeb_ApplicationLocation() is successful, the return value is E_SUCCEESS. In the 
 *          event of a failure, the return value is negative HRESULT. For extended error information,
 *          consult the list of documented Windows error codes. This API returns no eSellerate error codes.  
 *          If dwBufferLength (in bytes) is smaller than the path, the path is truncated to fit dwBufferLength. 
 *          
 * 
 */
HRESULT __stdcall eWeb_ApplicationLocation (
    LPSTR szPath,                             // [out] File location to be filled as a result. 
    DWORD dwBufferLength                      // [in] The size (in bytes) of space allocated for 'szPath'. 
);

/* NAME:    eWeb_WebStoreURL()
 * 
 * DESCRIPTION: The eWeb_WebStoreURL function retrieves an affiliate-specific URL stored on the user's 
 *          machine by eSellerate SalesTrac Technology (EST). 
 *
 * RETURNS: If eWeb_WebStoreURL() is successful, it returns E_SUCCESS. If the function fails, 
 *          the return value is a negative HRESULT. For extended error information consult the 
 *          list of documented eSellerate or Windows error codes. If the buffer length specified by
 *          iResultBufferSize is too small, the function returns E_SDK_BAD_PARAMETER. 
 * 
 */
HRESULT __stdcall eWeb_WebStoreURL (
    LPCSTR szPublisherID,                     // [in] Publisher ID Code. 
    LPCSTR szSkuRefNum,                       // [in] Sku RefNum Code.
    LPSTR  szResultBuffer,                    // [out] Buffer to be Filled as Result (always needed).
    INT    iResultBufferSize                  // [in] Size (in bytes) of 'szResultBuffer'. 
);

/* NAME:    eWeb_NewPrefillData()
 * 
 * DESCRIPTION: The eWeb_NewPrefillData function allocates a memory region for use by
 *          prefill data. Before passing prefill data to an Embedded WebStore API, you must
 *          allocate memory for it using eWeb_NewPrefillData. 
 *
 * RETURNS: A handle representing the internal prefill data region. If the 
 *          API fails, a NULL value is returned.
 * 
 */
BYTE* __stdcall eWeb_NewPrefillData(VOID);

/* NAME:    eWeb_DisposePrefillData()
 * 
 * DESCRIPTION: The eWeb_DisposePrefillData function disposes of memory allocated by 
 *          eWeb_NewPrefillData(). You must call this function on the prefill data for 
 *          it to be properly disposed. 
 *
 * RETURNS: No return value. 
 * 
 */
VOID eWeb_DisposePrefillData(
    BYTE *bypPrefillData                      // [in] The BYTE* returned from eWeb_NewPrefillData(). 
);

/* NAME:    eWeb_AddPrefillData()
 * 
 * DESCRIPTION: The eWeb_AddPrefillData function adds prefill data to the 
 *          internal buffer allocated by eWeb_NewPrefillData(). You must
 *          call this function once for every name/value pair you wish to 
 *          add to prefill data. 
 *
 * RETURNS: If the name/value pair is successfully added to the prefill data by
 *          eWeb_AddPrefillData(), it returns E_SUCCESS. If the name/value pair 
 *          cannot be added to the prefill data, then eWeb_AddPrefillData() returns
 *          E_SDK_CREATE_ENTRY_ERROR. If an error occurs related to insufficient 
 *          system resources or other OS-specific reasons, a negative HRESULT is returned.
 *          For extended error information, consult the documented Windows error codes. 
 * 
 */
HRESULT __stdcall eWeb_AddPrefillData(
     BYTE   *bypPrefillData,                  // [in] Prefill Data Buffer returned by eWeb_NewPrefillData().
     LPCSTR szName,                           // [in] The name of the eSellerate prefill variable being set. 
     LPCSTR szValue                           // [in] The value being given to the eSellerate prefill variable. 
);

/* NAME:    eWeb_GetResultDataValueLength()
 * 
 * DESCRIPTION: The eWeb_GetResultDataValueLength function retrieves the length
 *          (in bytes) of a result value given its name. eWeb_GetResultDataValueLength()
 *          is a utility function for properly allocating memory in which to store
 *          result data. Call this API before reading in a result data value so as to 
 *          prevent buffer overflows. 
 *
 * RETURNS: If the length is successfully retrieved, then eWeb_GetResultDataValueLength()
 *          returns E_SUCCESS. If the name cannot be found within the result data, 
 *          this function returns E_SDK_OBJECT_NOT_FOUND. This function returns no 
 *          Windows error codes.          
 * 
 */
HRESULT __stdcall eWeb_GetResultDataValueLength(
     BYTE   *bypResultData,                   // [in] The result data returned from the Purchase or CheckForUpdate family of functions.                 
     LPCSTR szName,                           // [in] The name of the result data variable for which to search. 
     WORD   *wpLength                         // [out] The length of the variable's value. 
);

/* NAME:    eWeb_GetResultDataValue()
 * 
 * DESCRIPTION: The eWeb_GetResultDataValue function retrieves the value of a given
 *          result data variable. In order to allocate enough memory for 
 *          the result data variable's value, call eWeb_GetResultDataValueLength(). 
 *
 * RETURNS: If the value is successfully retrieved, eWeb_GetResultDataValue() returns
 *          E_SUCCESS. If the result data variable cannot be found, it returns
 *          E_SDK_OBJECT_NOT_FOUND. This function returns no Windows error codes. 
 * 
 */
HRESULT __stdcall eWeb_GetResultDataValue(
     BYTE   *bypResultData,                   // [in] The result data returned from the Purchase or CheckForUpdate family of functions.  
     LPCSTR szName,                           // [in] The name of the result data variable for which to search.
     LPSTR  szValue,                          // [out] The memory region to hold the result data variable's value. 
     WORD   wValueLen                         // [in] The length of the memory region to hold the result data variable's value.
);

/* NAME:    eWeb_DisposeResultData()
 * 
 * DESCRIPTION: The eWeb_DisposeResultData function disposes of memory allocated by 
 *          API that return result data. You must call this function on the result 
 *          data for it to be properly disposed.  
 *
 * RETURNS: No return value. 
 * 
 */
VOID _stdcall eWeb_DisposeResultData(
     BYTE *bypResultData                      // [in] The result data returned from the Purchase or CheckForUpdate family of functions. 
);

/* NAME:    eWeb_IndexGetResultDataValueLength()
 * 
 * DESCRIPTION: The eWeb_IndexGetResultDataValueLength function retrieves the length
 *          (in bytes) of a result value given the combination of record and field name and
 *          index value. 
 *
 * RETURNS: eWeb_IndexGetResultDataValueLength() returns E_SUCCESS if the value was found. 
 *          If the function cannot find the given variable, E_SDK_OBJECT_NOT_FOUND is returned. 
 *          This function returns no Windows error codes. 
 * 
 */
HRESULT __stdcall eWeb_IndexGetResultDataValueLength(
     BYTE   *bypResultData,                   // [in] The result data returned from the Purchase or CheckForUpdate family of functions.  
     LPCSTR szRecord,                         // [in] The name of the record which contains the value whose length is being searched.
     LPCSTR szField,                          // [in] The name of the field whose length you wish to identify. 
     WORD   wIndex,                           // [in] The zero-based index of the record/field instance. 
     WORD   *wpLength                         // [out] The length (in bytes) of the requested record and field. 
);

/* NAME:    eWeb_IndexGetResultDataValue()
 * 
 * DESCRIPTION: The eWeb_IndexGetResultDataValue function returns the data 
 *          for the supplied record/field combination. In order to 
 *          provide enough memory for the resultant data value, 
 *          call eWeb_IndexGetResultDataValueLength(). 
 *
 * RETURNS: eWeb_IndexGetResultDataValue() returns E_SUCCESS if the 
 *          result data was successfully located, otherwise 
 *          E_SDK_OBJECT_NOT_FOUND. This function returns no Windows
 *          error codes. 
 * 
 */
HRESULT __stdcall eWeb_IndexGetResultDataValue(
     BYTE   *bypResultData,                   // [in] The result data returned from the Purchase or CheckForUpdate family of functions.  
     LPCSTR szRecord,                         // [in] The record name containing the variable being searched. 
     LPCSTR szField,                          // [in] The field name within the record for which you wish to search. 
     WORD   wIndex,                           // [in] The zero-based index of the name instance. 
     LPSTR  szValue,                          // [out] The buffer to receive the value upon success. 
     WORD   wValueLen                         // [in] The length of the buffer to receive the value upon success. 
);

/* NAME:    eWeb_GetOrderItemByIndex()
 * 
 * DESCRIPTION: The eWeb_GetOrderItemByIndex function returns the order
 *          item data at the given zero-based index. Each output buffer must
 *          be either NULL (in which case it will not be retrieved), or at least
 *          256 bytes in length. Values greater than 256 will be truncated.
 *          Buffers that are smaller than 256 bytes in length will cause
 *          undefined behavior. 
 *
 * RETURNS: If any values are located, eWeb_GetOrderItemByIndex() returns TRUE. 
 *          If no values are found, this function returns FALSE. 
 * 
 */

BOOL __stdcall eWeb_GetOrderItemByIndex(
     BYTE  *bypResultData,                     // [in] The result data returned from the Purchase or CheckForUpdate family of functions. 
     WORD  wIndex,                             // [in] The zero-based index of the order item for which you are searching. 
     LPSTR szSkuRefNum,                        // [out] The original SKU purchased (optional). 
     LPSTR szRedirectSkuRefNum,                // [out] The skuRefNum that was actually purchased in case of SKU redirect (optional). 
     LPSTR szQuantity,                         // [out] The quanity of the item being purchased (optional). 
     LPSTR szRegistrationName,                 // [out] The name on which the serial number was based (optional). 
     LPSTR szSerialNumber,                     // [out] The serial number associated with this purchase (optional). 
     LPSTR szPromptedValue,                    // [out] The prompted value entered by the user given one was requested (optional). 
     LPSTR szActivationID,                     // [out] The activation ID for the product given that an activation for the product was requested (optional). 
     LPSTR szActivationKey,                    // [out] The activation Key for the product given that an activation for the product was requested (optional). 
     LPSTR szDownloadURL                       // [out] The eSellerate URL from which this product may be downloaded (valid for a short period of time) (optional).
);
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef __cplusplus
}
#endif

#endif
