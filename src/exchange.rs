use arboard::*;
use image::{DynamicImage, ImageBuffer};

/// in GNOME, screenshot tool saves the image into ~/Pictures instead of clipboard
pub fn from_last_picture() -> Option<DynamicImage> {
    let img_dir = dirs::picture_dir()?;

    // get the latet modified PNG file in the directory
    let mut latest = None;
    let mut latest_time = 0;
    for entry in std::fs::read_dir(img_dir).ok()? {
        let entry = entry.ok()?;
        let path = entry.path();
        if path.extension().unwrap_or_default() != "png" {
            continue;
        }
        let meta = match entry.metadata() {
            Ok(x) => x,
            Err(_) => continue,
        };
        let modified = match meta.modified() {
            Ok(x) => x,
            Err(_) => continue,
        };
        let modified = match modified.duration_since(std::time::SystemTime::UNIX_EPOCH) {
            Ok(x) => x.as_secs(),
            Err(_) => continue,
        };
        if modified > latest_time {
            latest_time = modified;
            latest = Some(path);
        }
    }
    if let Some(latest) = latest {
        // read as PNG image
        return match image::open(latest) {
            Ok(im) => Some(im),
            Err(_) => return None,
        };
    }
    None
}

/// get image from clipboard
pub fn from_clipboard() -> Option<DynamicImage> {
    let mut clipboard = match Clipboard::new() {
        Ok(c) => c,
        Err(_) => {
            return None;
        }
    };
    let im = match clipboard.get_image() {
        Ok(im) => im,
        Err(_) => return None,
    };
    // convert to DynamicImage
    let image = ImageBuffer::from_raw(
        im.width.try_into().unwrap(),
        im.height.try_into().unwrap(),
        im.bytes.into_owned(),
    )?;
    let out = DynamicImage::ImageRgba8(image);
    Some(out)
}

/// copy image to clipboard
pub fn to_clipboard(im: &DynamicImage) -> anyhow::Result<()> {
    let mut clipboard = Clipboard::new()?;
    let image = im.to_rgba8();
    let bytes = image.into_raw();
    let img_data = ImageData {
        bytes: bytes.into(),
        width: im.width() as usize,
        height: im.height() as usize,
    };
    clipboard.set_image(img_data)?;
    Ok(())
}
