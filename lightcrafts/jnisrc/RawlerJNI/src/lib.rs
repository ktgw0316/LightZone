use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jint, jshort, jsize};
use jni::JNIEnv;

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
