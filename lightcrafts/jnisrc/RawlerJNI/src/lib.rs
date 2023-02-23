mod lightzone;

use std::path::PathBuf;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jint, jshort, jshortArray, jsize};
use jni::JNIEnv;
use rawler::decoders::RawDecodeParams;
use rawler::RawImage;
use crate::lightzone::raw_to_prophoto_rgb;

fn get_image(env: JNIEnv, file: JString) -> RawImage {
    let file: String = env
        .get_string(file)
        .expect("Couldn't get java string!")
        .into();
    rawler::decode_file(&file).unwrap()
}

#[no_mangle]
pub extern "system" fn Java_com_lightcrafts_utils_Rawler_getRawWidth<'local>(
    env: JNIEnv<'local>,
    _class: JClass,
    file: JString<'local>,
) -> jint {
    let area = get_image(env, file).crop_area.unwrap();
    area.d.w as jint
}

#[no_mangle]
pub extern "system" fn Java_com_lightcrafts_utils_Rawler_getRawHeight<'local>(
    env: JNIEnv<'local>,
    _class: JClass,
    file: JString<'local>,
) -> jint {
    let area = get_image(env, file).crop_area.unwrap();
    area.d.h as jint
}

#[no_mangle]
pub extern "system" fn Java_com_lightcrafts_utils_Rawler_getRawData<'local>(
    env: JNIEnv<'local>,
    _class: JClass,
    file: JString<'local>,
) -> jshortArray {
    let image = get_image(env, file);
    let data = match image.data {
        rawler::RawImageData::Integer(data) => {
            data.iter().map(|&x| x as jshort).collect::<Vec<_>>()
        }
        _ => {
            eprintln!("Don't know how to process non-integer raw files");
            Vec::new()
        }
    };
    let buffer = env.new_short_array(data.len() as jsize).unwrap();
    env.set_short_array_region(buffer, 0, &data).unwrap();
    buffer
    // let buffer_obj = unsafe { JObject::from_raw(buffer) };
    // let data_jvalue = JValue::from(buffer_obj);
}

#[no_mangle]
pub extern "system" fn Java_com_lightcrafts_utils_Rawler_getProPhotoRGB<'local>(
    env: JNIEnv<'local>,
    _class: JClass,
    file: JString<'local>,
) -> jshortArray {
    let file: String = env
        .get_string(file)
        .expect("Couldn't get java string!")
        .into();
    let params = RawDecodeParams::default();
    let (image, dim) = raw_to_prophoto_rgb(&PathBuf::from(file), params).unwrap();
    let image = image.iter()
        .map(|&x| x as jshort)
        .collect::<Vec<_>>();
    let output = env.new_short_array(image.len() as jsize).unwrap();
    env.set_short_array_region(output, 0, &image).unwrap();
    println!("Returning {} x {} pixels", dim.w, dim.h);
    output
}



#[no_mangle]
pub extern "system" fn Java_com_lightcrafts_utils_Rawler_decode<'local>(
    env: JNIEnv<'local>,
    _class: JClass,
    file: JString<'local>,
) -> JObject<'local> {
    let file: String = env
        .get_string(file)
        .expect("Couldn't get java string!")
        .into();

    let image = rawler::decode_file(&file).unwrap();

    let class = env
        .find_class("com/lightcrafts/utils/Rawler/RawlerRawImage")
        .expect("Couldn't find java class!");

    let make = JValue::from(env.new_string(image.make).unwrap());
    let model = JValue::from(env.new_string(image.model).unwrap());
    let clean_make = JValue::from(env.new_string(image.clean_make).unwrap());
    let width = JValue::Int(image.width as jint);
    let height = JValue::Int(image.height as jint);
    let cpp = JValue::Int(image.cpp as jint);
    let bps = JValue::Int(image.bps as jint);

    let data = match image.data {
        rawler::RawImageData::Integer(data) => {
            data.iter().map(|&x| x as jshort).collect::<Vec<_>>()
        }
        _ => {
            eprintln!("Don't know how to process non-integer raw files");
            Vec::new()
        }
    };

    let buffer = env.new_short_array(data.len() as jsize).unwrap();
    env.set_short_array_region(buffer, 0, &data).unwrap();
    let buffer_obj = unsafe { JObject::from_raw(buffer) };
    let data_jvalue = JValue::from(buffer_obj);

    let ret = env.new_object(
            class,
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIII[S)Lcom/lightcrafts/utils/Rawler/RawlerRawImage;",
            &[ make, model, clean_make, width, height, cpp, bps, data_jvalue ],
        )
        .expect("Couldn't create java object!");
    ret
}

