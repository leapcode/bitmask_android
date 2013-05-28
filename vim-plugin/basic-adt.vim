function! AndroidRun()
	let new_project_root = s:Find_in_parent("AndroidManifest.xml", s:windowdir() ,$HOME)
	echo "Found Android Project at: " . new_project_root
	echo "Windowdir: " . s:windowdir()
	if new_project_root != "Nothing"
		let b:project_root = new_project_root
		new
		set buftype=nofile
		silent! exec "r!../run.sh SII b:project_root
	endif
endfunc

" Find_in_parent
" " find the file argument and returns the path to it.
" " Starting with the current working dir, it walks up the parent folders
" " until it finds the file, or it hits the stop dir.
" " If it doesn't find it, it returns "Nothing"
function s:Find_in_parent(fln,flsrt,flstp)
	let here = a:flsrt
	while ( strlen( here) > 0 )
		if filereadable( here . "/" . a:fln )
			return here
		endif
		let fr = match(here, "/[^/]*$")
		if fr == -1
			break
		endif
		let here = strpart(here, 0, fr)
		if here == a:flstp
			break
		endif
	endwhile
	return "Nothing"
endfunc

" windowdir
" " Gets the directory for the file in the current window
" " Or the current working dir if there isn't one for the window.
" " Use tr to allow that other OS paths, too
function s:windowdir()
	if winbufnr(0) == -1
		let unislash = getcwd()
	else
		let unislash = fnamemodify(bufname(winbufnr(0)), ':p:h')
	endif
	return tr(unislash, '\', '/')
endfunc
