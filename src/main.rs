pub mod render;

use std::sync::{Arc, Mutex};
use winit::event::{
    ElementState, Event, KeyboardInput, ModifiersState, MouseButton, VirtualKeyCode, WindowEvent,
};
use winit::event_loop::{ControlFlow, EventLoop};
use winit::window::{WindowBuilder, WindowLevel};

#[derive(Debug, Clone)]
pub enum Readiness {
    ToCrop,
    ToDrawArrow,
    ToDrawLine,
}

#[derive(Default, Debug, Clone)]
pub enum DrawState {
    #[default]
    NoImage,
    ImageIsReady {
        ready: Readiness,
    },
    CropStarted {
        x: u32,
        y: u32,
        m: Option<(u32, u32)>,
    },
    ArrowStarted {
        x: u32,
        y: u32,
        m: Option<(u32, u32)>,
    },
    LineStarted {
        x: u32,
        y: u32,
        m: Option<(u32, u32)>,
    },
}

#[derive(Default, Debug, Clone)]
pub struct AppState {
    pub mouse_left: bool,
    pub mouse: Option<(u32, u32)>,
    pub img: Option<image::DynamicImage>,
    pub draw_state: DrawState,
}

impl AppState {
    pub fn new() -> Self {
        // get the image from the pictures folder
        AppState::default()
    }

    /// returns true if the window needs to be redrawn
    pub fn on_keypress(&mut self, key: VirtualKeyCode, modifiers: ModifiersState) -> bool {
        match key {
            VirtualKeyCode::Escape => {
                self.draw_state = DrawState::NoImage;
                self.img = None;
                return true;
            }
            VirtualKeyCode::V => {
                if modifiers.ctrl() {
                    // paste image from clipboard
                }
            }
            VirtualKeyCode::C => {
                if modifiers.ctrl() {
                    // copy image to clipboard
                }
            }
            VirtualKeyCode::Key1 => {
                self.draw_state = DrawState::ImageIsReady {
                    ready: Readiness::ToCrop,
                }
            }
            VirtualKeyCode::Key2 => {
                self.draw_state = DrawState::ImageIsReady {
                    ready: Readiness::ToDrawArrow,
                }
            }
            VirtualKeyCode::Key3 => {
                self.draw_state = DrawState::ImageIsReady {
                    ready: Readiness::ToDrawLine,
                }
            }
            _ => {}
        };
        false
    }

    pub fn on_mouse_left(&mut self, pressed: bool) -> bool {
        self.mouse_left = pressed;
        if pressed {
            match &self.draw_state {
                DrawState::ImageIsReady { ready } => match ready {
                    Readiness::ToCrop => {
                        if let Some((x, y)) = self.mouse {
                            self.draw_state = DrawState::CropStarted { x, y, m: None }
                        }
                    }
                    Readiness::ToDrawArrow => {
                        if let Some((x, y)) = self.mouse {
                            self.draw_state = DrawState::ArrowStarted { x, y, m: None }
                        }
                    }
                    Readiness::ToDrawLine => {
                        if let Some((x, y)) = self.mouse {
                            self.draw_state = DrawState::LineStarted { x, y, m: None }
                        }
                    }
                },
                DrawState::CropStarted { x, y, m } => {
                    // TODO: replace image with cropped image
                }
                DrawState::ArrowStarted { x, y, m } => {
                    // TODO: replace image with a new, draw an arrow
                }
                DrawState::LineStarted { x, y, m } => {
                    // TODO: replace image with a new, draw a line
                }
                _ => {}
            }
        }
        false
    }

    pub fn on_mouse_move(&mut self, mouse_x: u32, mouse_y: u32) -> bool {
        self.mouse = Some((mouse_x, mouse_y));
        match self.draw_state {
            DrawState::CropStarted { x, y, .. } => {
                self.draw_state = DrawState::CropStarted {
                    x,
                    y,
                    m: Some((mouse_x, mouse_y)),
                };
                return true;
            }
            DrawState::ArrowStarted { x, y, .. } => {
                self.draw_state = DrawState::ArrowStarted {
                    x,
                    y,
                    m: Some((mouse_x, mouse_y)),
                };
                return true;
            }
            DrawState::LineStarted { x, y, .. } => {
                self.draw_state = DrawState::LineStarted {
                    x,
                    y,
                    m: Some((mouse_x, mouse_y)),
                };
                return true;
            }
            _ => {}
        }
        false
    }
}

fn main() {
    let scale = 1 as usize;
    let event_loop = EventLoop::new();
    let height = 480;
    let sz = height / scale as u32;
    let pad = 20u32;
    let top_bar_height = 40;
    let screen_size = (1920, 1080);
    let title = "Quick Image Highlighter";
    let inner_size = winit::dpi::PhysicalSize::new(sz, sz);

    let window = WindowBuilder::new()
        .with_title(title)
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
    window.set_window_level(WindowLevel::AlwaysOnTop);

    let picture = "/home/adm0/Pictures/secrets.png";
    // TODO: pick the last image from pictures folder
    let im = image::open(picture).unwrap();
    let app_state = AppState {
        img: Some(im),
        draw_state: DrawState::ImageIsReady {
            ready: Readiness::ToCrop,
        },
        ..Default::default()
    };
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
