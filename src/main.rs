pub mod exchange;
pub mod render;
pub mod state;
pub mod transform;

use state::*;
use std::sync::{Arc, Mutex};
use winit::event::{ElementState, Event, KeyboardInput, ModifiersState, MouseButton, WindowEvent};
use winit::event_loop::{ControlFlow, EventLoop};
use winit::window::{Icon, WindowBuilder, WindowLevel};

fn load_icon() -> Option<Icon> {
    let (icon_rgba, icon_width, icon_height) = {
        let buf = include_bytes!("../public/favicon.png");
        let image = image::load_from_memory_with_format(buf, image::ImageFormat::Png)
            .expect("Failed load window icon")
            .into_rgba8();
        let (width, height) = image.dimensions();
        let rgba = image.into_raw();
        (rgba, width, height)
    };
    Some(Icon::from_rgba(icon_rgba, icon_width, icon_height).expect("Failed to open icon"))
}

fn main() {
    let scale = 1 as usize;
    let event_loop = EventLoop::new();
    let height = 480;
    let sz = height / scale as u32;
    let pad = 20u32;
    let top_bar_height = 40;
    let screen_size = (1920, 1080); // can be detected from the active monitor
    let title = "Quick Image Highlighter";
    let inner_size = winit::dpi::PhysicalSize::new(sz, sz);

    let window = WindowBuilder::new()
        .with_title(title)
        .with_window_icon(load_icon())
        .with_window_level(WindowLevel::Normal)
        .with_inner_size(inner_size)
        .with_resizable(true)
        .with_transparent(false)
        .with_position(winit::dpi::PhysicalPosition::new(
            screen_size.0 - sz - pad,
            screen_size.1 - sz - pad - top_bar_height,
        ))
        .build(&event_loop)
        .unwrap();
    let wid = window.id();
    let app_state = match exchange::from_last_picture() {
        Some(im) => AppState {
            img: Some(im),
            draw_state: DrawState::ImageIsReady {
                ready: Readiness::ToCrop,
            },
            ..Default::default()
        },
        None => AppState::default(),
    };
    window.set_title(&app_state.get_title());
    let astate = Arc::new(Mutex::new(app_state));
    let mut key_modifiers = ModifiersState::default();
    event_loop.run(move |event, _, control_flow| {
        *control_flow = ControlFlow::Wait;

        match event {
            Event::RedrawRequested(window_id) if window_id == wid => {
                if let Ok(state) = astate.clone().lock() {
                    crate::render::draw(&window, &state);
                }
            }
            Event::WindowEvent {
                event: WindowEvent::ModifiersChanged(m_state),
                ..
            } => {
                key_modifiers = m_state;
            }
            Event::WindowEvent {
                event:
                    WindowEvent::KeyboardInput {
                        input:
                            KeyboardInput {
                                state: ElementState::Pressed,
                                virtual_keycode: Some(keypress),
                                ..
                            },
                        ..
                    },
                ..
            } => {
                if let Ok(mut state) = astate.clone().lock() {
                    if state.on_keypress(keypress, key_modifiers) {
                        window.set_title(&state.get_title());
                        window.request_redraw();
                    }
                }
            }
            Event::WindowEvent {
                event: WindowEvent::MouseInput { state, button, .. },
                ..
            } => {
                let pressed = match state {
                    ElementState::Pressed => true,
                    ElementState::Released => false,
                };
                match button {
                    MouseButton::Left => {
                        if let Ok(mut state) = astate.clone().lock() {
                            if state.on_mouse_left(pressed) {
                                window.set_title(&state.get_title());
                                window.request_redraw();
                            }
                        }
                    }
                    _ => {}
                };
            }
            Event::WindowEvent {
                event: WindowEvent::CursorMoved { position, .. },
                ..
            } => {
                if let Ok(mut state) = astate.clone().lock() {
                    if state.on_mouse_move(position.x as u32, position.y as u32) {
                        window.set_title(&state.get_title());
                        window.request_redraw();
                    }
                }
            }
            Event::WindowEvent {
                event: WindowEvent::CloseRequested,
                window_id,
            } if window_id == wid => {
                *control_flow = ControlFlow::Exit;
            }
            _ => {} // window.request_redraw(),
        }
    });
}
