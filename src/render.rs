use crate::state::*;

use image::GenericImageView;
use softbuffer::{Buffer, Context, Surface};
use std::cell::RefCell;
use std::collections::HashMap;
use std::mem::ManuallyDrop;
use std::num::NonZeroU32;
use winit::window::{Window, WindowId};

fn draw_rectangle(buffer: &mut Buffer, sz_width: u32, x1: u32, y1: u32, x2: u32, y2: u32) {
    let x = std::cmp::min(x1, x2);
    let y = std::cmp::min(y1, y2);
    let w = std::cmp::max(x1, x2) - x;
    let h = std::cmp::max(y1, y2) - y;
    // draw crop rectangle
    for i in 0..h {
        let color = 0xFFFF00FF;
        buffer[((y + i) * sz_width + x) as usize] = color;
        buffer[((y + i) * sz_width + x + w) as usize] = color;
    }
    for i in 0..w {
        let color = 0xFF0000FF;
        buffer[(y * sz_width + x + i) as usize] = color;
        buffer[((y + h) * sz_width + x + i) as usize] = color;
    }
}

/// The graphics context used to draw to a window.
struct GraphicsContext {
    /// The global softbuffer context.
    context: Context,
    /// The hash map of window IDs to surfaces.
    surfaces: HashMap<WindowId, Surface>,
}

impl GraphicsContext {
    fn new(w: &Window) -> Self {
        Self {
            context: unsafe { Context::new(w) }.expect("Failed to create a softbuffer context"),
            surfaces: HashMap::new(),
        }
    }
    fn surface(&mut self, w: &Window) -> &mut Surface {
        self.surfaces.entry(w.id()).or_insert_with(|| {
            unsafe { Surface::new(&self.context, w) }
                .expect("Failed to create a softbuffer surface")
        })
    }
}

pub fn draw(window: &Window, state: &AppState) {
    thread_local! {
        // NOTE: You should never do things like that, create context and drop it before
        // you drop the event loop. We do this for brevity to not blow up examples. We use
        // ManuallyDrop to prevent destructors from running.
        //
        // A static, thread-local map of graphics contexts to open windows.
        static GC: ManuallyDrop<RefCell<Option<GraphicsContext>>> = ManuallyDrop::new(RefCell::new(None));
    }

    GC.with(|gc| {
        // Either get the last context used or create a new one.
        let mut gc = gc.borrow_mut();
        let surface = gc
            .get_or_insert_with(|| GraphicsContext::new(window))
            .surface(window);

        // Fill a buffer with a solid color.
        const DARK_GRAY: u32 = 0xFF909090;
        let size = window.inner_size();
        surface
            .resize(
                NonZeroU32::new(size.width).expect("Width must be greater than zero"),
                NonZeroU32::new(size.height).expect("Height must be greater than zero"),
            )
            .expect("Failed to resize the softbuffer surface");

        let mut buffer: Buffer = surface
            .buffer_mut()
            .expect("Failed to get the softbuffer buffer");
        buffer.fill(DARK_GRAY);

        if let Some(im) = &state.img {
            // draw image in the buffer
            let (width, height) = (im.width(), im.height());
            // println!("image: {:?}x{:?}", width, height);
            // for each pixel of the image
            for y in 0..height - 1 {
                if y >= size.height - 1 {
                    break;
                }
                for x in 0..width - 1 {
                    if x >= size.width - 1 {
                        break;
                    }
                    let pixel = im.get_pixel(x, y);
                    let r = pixel[0];
                    let g = pixel[1];
                    let b = pixel[2];
                    let a = pixel[3];
                    let color = (a as u32) << 24 | (r as u32) << 16 | (g as u32) << 8 | b as u32;
                    buffer[(y * size.width + x) as usize] = color;
                }
            }

            match &state.draw_state {
                DrawState::NoImage => {}
                DrawState::CropStarted { x, y, m } => {
                    if let Some((x2, y2)) = m {
                        draw_rectangle(&mut buffer, size.width, *x, *y, *x2, *y2);
                    }
                }

                _ => {}
            };
        }
        buffer
            .present()
            .expect("Failed to present the softbuffer buffer");
    })
}
