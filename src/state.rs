use winit::event::{ModifiersState, VirtualKeyCode};

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

    /// returns the title of the window
    pub fn get_title(&self) -> String {
        match &self.draw_state {
            DrawState::NoImage => {
                return "Please load image".to_string();
            }
            DrawState::ImageIsReady { ready } => match ready {
                Readiness::ToCrop => {
                    return "Crop the image".to_string();
                }
                Readiness::ToDrawArrow => {
                    return "Draw an arrow".to_string();
                }
                Readiness::ToDrawLine => {
                    return "Draw a line".to_string();
                }
            },
            DrawState::CropStarted { .. } => {
                return "Cropping the image".to_string();
            }
            DrawState::ArrowStarted { .. } => {
                return "Drawing an arrow".to_string();
            }
            DrawState::LineStarted { .. } => {
                return "Drawing a line".to_string();
            }
        };
    }

    /// returns true if the window needs to be redrawn
    pub fn on_keypress(&mut self, key: VirtualKeyCode, modifiers: ModifiersState) -> bool {
        match key {
            VirtualKeyCode::Escape => {
                self.draw_state = DrawState::NoImage;
                self.img = None;
                return true;
            }
            VirtualKeyCode::L => {
                if modifiers.ctrl() {
                    self.img = crate::exchange::from_last_picture();
                    if !self.img.is_none() {
                        self.draw_state = DrawState::ImageIsReady {
                            ready: Readiness::ToCrop,
                        }
                    } else {
                        self.draw_state = DrawState::NoImage;
                    }
                    return true;
                }
            }
            VirtualKeyCode::V => {
                if modifiers.ctrl() {
                    // paste image from clipboard
                    self.img = crate::exchange::from_clipboard();
                    if !self.img.is_none() {
                        self.draw_state = DrawState::ImageIsReady {
                            ready: Readiness::ToCrop,
                        }
                    } else {
                        self.draw_state = DrawState::NoImage;
                    }
                    return true;
                }
            }
            VirtualKeyCode::C => {
                if modifiers.ctrl() {
                    // copy image to clipboard
                    if let Some(img) = &self.img {
                        if let Err(e) = crate::exchange::to_clipboard(img) {
                            println!("Error copying to clipboard: {}", e);
                        }
                    }
                    return true;
                }
            }
            VirtualKeyCode::Key1 => {
                self.draw_state = DrawState::ImageIsReady {
                    ready: Readiness::ToCrop,
                };
                return true;
            }
            VirtualKeyCode::Key2 => {
                self.draw_state = DrawState::ImageIsReady {
                    ready: Readiness::ToDrawArrow,
                };
                return true;
            }
            VirtualKeyCode::Key3 => {
                self.draw_state = DrawState::ImageIsReady {
                    ready: Readiness::ToDrawLine,
                };
                return true;
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
                        let m = self.mouse.clone();
                        if let Some((x, y)) = self.mouse {
                            self.draw_state = DrawState::CropStarted { x, y, m }
                        }
                        return true;
                    }
                    Readiness::ToDrawArrow => {
                        let m = self.mouse.clone();
                        if let Some((x, y)) = self.mouse {
                            self.draw_state = DrawState::ArrowStarted { x, y, m }
                        }
                        return true;
                    }
                    Readiness::ToDrawLine => {
                        let m = self.mouse.clone();
                        if let Some((x, y)) = self.mouse {
                            self.draw_state = DrawState::LineStarted { x, y, m }
                        }
                        return true;
                    }
                },
                DrawState::CropStarted { x, y, m } => {
                    if let Some(img) = &self.img {
                        if let Some((x2, y2)) = m {
                            let xx1 = std::cmp::min(x, x2);
                            let xx2 = std::cmp::max(x, x2);
                            let yy1 = std::cmp::min(y, y2);
                            let yy2 = std::cmp::max(y, y2);
                            let im = crate::transform::cropped(img, *xx1, *yy1, *xx2, *yy2);
                            self.img = Some(im);
                            self.draw_state = DrawState::ImageIsReady {
                                ready: Readiness::ToCrop,
                            };
                            return true;
                        }
                    }
                }
                DrawState::ArrowStarted { x, y, m } => {
                    let _ = (x, y, m);
                    // TODO: replace image with a new, draw an arrow
                }
                DrawState::LineStarted { x, y, m } => {
                    let _ = (x, y, m);
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
