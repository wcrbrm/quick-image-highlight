use image::DynamicImage;

pub fn cropped(img: &DynamicImage, x: u32, y: u32, x2: u32, y2: u32) -> DynamicImage {
    let w = x2 - x + 1;
    let h = y2 - y + 1;
    let cropped = img.crop_imm(x, y, w, h);
    cropped
}
