package com.kirin.bilitv.core.network

object BiliApiEndpoints {
  const val ApiBase = "https://api.bilibili.com"
  const val PassportBase = "https://passport.bilibili.com"
  const val AppBase = "https://app.bilibili.com"

  const val WbiNav = "$ApiBase/x/web-interface/nav"
  const val BuvidSpi = "$ApiBase/x/frontend/finger/spi"
  const val BuvidActivate = "$ApiBase/x/internal/gaia-gateway/ExClimbWuzhi"
  const val Recommend = "$ApiBase/x/web-interface/wbi/index/top/feed/rcmd"
  const val Popular = "$ApiBase/x/web-interface/popular"
  const val Region = "$ApiBase/x/web-interface/dynamic/region"
  const val View = "$ApiBase/x/web-interface/view"
  const val ArchiveRelated = "$ApiBase/x/web-interface/archive/related"
  const val SpaceArcSearch = "$ApiBase/x/space/wbi/arc/search"
  const val Relation = "$ApiBase/x/relation"
  const val RelationModify = "$ApiBase/x/relation/modify"
  const val DynamicFeed = "$ApiBase/x/polymer/web-dynamic/v1/feed/all"
  const val HistoryCursor = "$ApiBase/x/web-interface/history/cursor"
  const val Search = "$ApiBase/x/web-interface/wbi/search/type"
  const val SearchSuggest = "https://s.search.bilibili.com/main/suggest"
  const val PlayUrl = "$ApiBase/x/player/wbi/playurl"
  const val PlayerVideoshot = "$ApiBase/x/player/videoshot"
  const val PlayerDanmaku = "$ApiBase/x/v1/dm/list.so"
  const val CommentBase = "https://comment.bilibili.com"
  const val SponsorBlockSkipSegments = "https://bsbsb.top/api/skipSegments"
  const val PlayerOnlineTotal = "$ApiBase/x/player/online/total"
  const val PlayerHeartbeat = "$ApiBase/x/click-interface/web/heartbeat"
  const val TvQrCode = "$PassportBase/x/passport-tv-login/qrcode/auth_code"
  const val TvQrPoll = "$PassportBase/x/passport-tv-login/qrcode/poll"

  fun legacyDanmaku(cid: Long): String = "$CommentBase/$cid.xml"
}
