execute pathogen#infect()
syntax on
filetype plugin indent on

set encoding=utf-8

set number
set background=dark
colorscheme solarized
highlight clear SignColumn

set laststatus=2
let g:airline#extensions#tabline#enabled = 1
let g:airline_theme = "solarized"

let g:airline_left_sep=''
let g:airline_right_sep=''

let g:gitgutter_sign_column_always = 1

autocmd BufWritePre * :%s/\s\+$//e
