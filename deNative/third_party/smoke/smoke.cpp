#include <openssl/ssl.h>
#include <curl/curl.h>
#include <boost/beast/websocket.hpp>

extern "C" int de_deps_smoke_version()
{
    return OPENSSL_VERSION_NUMBER + LIBCURL_VERSION_NUM;
}
