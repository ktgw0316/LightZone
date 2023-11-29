project('forkdaemon', 'cpp',
  default_options : ['warning_level=3', 'cpp_std=c++14'])

target_os = target_machine.system()
if target_os in ['linux', 'darwin', 'freebsd']
  add_project_arguments('-DUES_FORKER', language : 'cpp')
endif

executable('LightZone-forkd', 'forkdaemon.cpp', install : true)
