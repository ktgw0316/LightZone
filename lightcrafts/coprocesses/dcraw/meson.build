project('dcraw', 'c',
  version : '9.28',
  default_options : ['warning_level=3'])

target_os = target_machine.system()
defines = ['-DNODEPS', '-DLIGHTZONE']
if target_os == 'windows'
  # cflags = '-fopenmp -static'
  defines += '-DDJGPP'
  link = ['-lmingwex', '-lws2_32']
elif target_os == 'darwin'
  # cflags = '-Xpreprocessor -fopenmp'
  link = '-lomp'
elif target_os in ['linux', 'freebsd']
  # cflags = '-fopenmp'
  link = '-lm'
endif
# add_project_arguments(cflags, defines, language : 'c')
add_project_arguments(defines, language : 'c')
add_project_link_arguments(link, language : 'c')

executable('dcraw_lz',
  'dcraw_lz.c',
  dependencies : dependency('openmp'),
  install : true)
