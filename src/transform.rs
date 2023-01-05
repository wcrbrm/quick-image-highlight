use image::{DynamicImage, GenericImage, ImageBuffer};
use imageproc::drawing::*;

pub fn cropped(img: &DynamicImage, x: u32, y: u32, x2: u32, y2: u32) -> DynamicImage {
    let w = x2 - x + 1;
    let h = y2 - y + 1;
    let cropped = img.crop_imm(x, y, w, h);
    cropped
}

pub fn with_line(img: &DynamicImage, x: u32, y: u32, x2: u32, y2: u32) -> DynamicImage {
    let color = image::Rgba([255, 0, 0, 0]);
    let mut out = ImageBuffer::new(img.width(), img.height());
    out.copy_from(img, 0, 0).unwrap();
    draw_line_segment_mut(
        &mut out,
        (x as f32, y as f32),
        (x2 as f32, y2 as f32),
        color,
    );
    DynamicImage::ImageRgba8(out)
}
