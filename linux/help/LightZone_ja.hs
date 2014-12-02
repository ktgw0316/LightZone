<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
         "../dtd/helpset_2_0.dtd">

<helpset version="2.0" xml:lang="ja">

  <!-- title -->
  <title>LightZone - ヘルプ</title>

  <!-- maps -->
  <maps>
     <homeID>top</homeID>
     <mapref location="Japanese/Map.jhm"/>
  </maps>

  <!-- views -->
  <view>
    <name>TOC</name>
    <label>目次</label>
    <type>javax.help.TOCView</type>
    <data>Japanese/LightZoneTOC.xml</data>
  </view>

  <view>
    <name>Search</name>
    <label>検索</label>
    <type>javax.help.SearchView</type>
    <data engine="com.sun.java.help.search.DefaultSearchEngine">
      Japanese/JavaHelpSearch
    </data>
  </view>

  <presentation default="true" displayviewimages="false">
     <name>main window</name>
     <size width="700" height="400" />
     <location x="200" y="200" />
     <title>LightZone - オンライン ヘルプ</title>
     <toolbar>
        <helpaction>javax.help.BackAction</helpaction>
        <helpaction>javax.help.ForwardAction</helpaction>
        <helpaction>javax.help.SeparatorAction</helpaction>
        <helpaction>javax.help.HomeAction</helpaction>
        <helpaction>javax.help.ReloadAction</helpaction>
        <helpaction>javax.help.SeparatorAction</helpaction>
        <helpaction>javax.help.PrintAction</helpaction>
        <helpaction>javax.help.PrintSetupAction</helpaction>
     </toolbar>
  </presentation>
  <presentation>
     <name>main</name>
     <size width="400" height="400" />
     <location x="200" y="200" />
     <title>LightZone - オンライン ヘルプ</title>
  </presentation>
</helpset>
