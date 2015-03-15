execute pathogen#infect()
syntax on
filetype plugin indent on

set encoding=utf-8
set number
set background=dark
colorscheme solarized

" git gutter setting
highlight clear SignColumn

" airline settings
set laststatus=2
let g:airline#extensions#tabline#enabled = 1
let g:airline_theme = "solarized"

let g:airline_left_sep=''
let g:airline_right_sep=''

let g:gitgutter_sign_column_always = 1

" remove whitespace
autocmd BufWritePre * :%s/\s\+$//e

" clojure syntax for .boot
au BufRead,BufNewFile *.boot set filetype=clojure

" moving between windows works by ctrl + direction
map <C-j> <C-W>j
map <C-k> <C-W>k
map <C-h> <C-W>h
map <C-l> <C-W>l

" j and k work like normal editors do with line wrap
map j gj
map k gk

" hidden makes it hide buffers instead of closing them when you can't see them
set backspace=indent,eol,start
set hidden

set colorcolumn=80

" no-op on arrow keys
map <up> <nop>
map <down> <nop>
map <left> <nop>
map <right> <nop>

" i hate shift keys
nnoremap ; :
