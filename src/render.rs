use crate::state::*;

use image::GenericImageView;
use softbuffer::{Context, Surface};
use std::cell::RefCell;
use std::collections::HashMap;
use std::mem::ManuallyDrop;
use std::num::NonZeroU32;
use winit::window::{Window, WindowId};

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

        let mut buffer = surface
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
        }
        buffer
            .present()
            .expect("Failed to present the softbuffer buffer");
    })
}
