#
# Copyright (C) Mellanox Technologies Ltd. 2021. ALL RIGHTS RESERVED.
#
# See file LICENSE for terms.
#

#
# Check for Golang support
#

go_happy="no"
AC_ARG_WITH([go],
            [AC_HELP_STRING([--with-go=(PATH)],
                            [Compile GO UCX bindings (default is guess).])
            ], [], [with_go=guess])

AS_IF([test "x$with_go" != xno],
      [
       AC_CHECK_PROG(GOBIN,  go,  yes)

       AS_IF([test "x${GOBIN}" = "xyes"],
       [go_happy="yes"],
       [
        AS_IF([test "x$with_go" = "xguess"],
            [AC_MSG_WARN([Disabling GO support - GO compiler not in path.])],
            [AC_MSG_ERROR([GO support was explicitly requested, but go compiler not in path.])])
       ])

      ],
      [AC_MSG_WARN([GO support was explicitly disabled.])])

AM_CONDITIONAL([HAVE_GO], [test "x$go_happy" != "xno"])
AM_COND_IF([HAVE_GO],
           [AC_SUBST([GO], ["go"])
           build_bindings="${build_bindings}:go"]
          )
