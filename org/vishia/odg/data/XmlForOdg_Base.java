package org.vishia.odg.data;

import java.util.LinkedList;
import java.util.List;
import org.vishia.genJavaOutClass.SrcInfo;

import org.vishia.odg.data.XmlForOdg.*;

/**This file is generated by genJavaOut.jzTc script. */
public class XmlForOdg_Base extends SrcInfo {

    
    protected Office_document_content office_document_content;
    
    
    
    
    /**Access to parse result.*/
    public Office_document_content get_office_document_content() { return office_document_content; }
    
    



  /**Class for Component Office_document_content. */
  public static class Office_document_content_Base extends SrcInfo {
  
  
    
    protected String office_version;
    
    
    
    protected Office_automatic_styles office_automatic_styles;
    
    
    
    protected Office_body office_body;
    
    
    
    protected Office_font_face_decls office_font_face_decls;
    
    
    
    protected String office_scripts;
    
    
    
    
    /**Access to parse result.*/
    public String get_office_version() { return office_version; }
    
    
    
    
    /**Access to parse result.*/
    public Office_automatic_styles get_office_automatic_styles() { return office_automatic_styles; }
    
    
    
    
    /**Access to parse result.*/
    public Office_body get_office_body() { return office_body; }
    
    
    
    
    /**Access to parse result.*/
    public Office_font_face_decls get_office_font_face_decls() { return office_font_face_decls; }
    
    
    
    
    /**Access to parse result.*/
    public String get_office_scripts() { return office_scripts; }
    
    
  
  }




  /**Class for Component Office_automatic_styles. */
  public static class Office_automatic_styles_Base extends SrcInfo {
  
  
    
    protected List<Style_style> style_style;
    
    
    
    protected Text_list_style text_list_style;
    
    
    
    
    /**Access to parse result, get the elements of the container style_style*/
    public Iterable<Style_style> get_style_style() { return style_style; }
    
    /**Access to parse result, get the size of the container style_style.*/
    public int getSize_style_style() { return style_style ==null ? 0 : style_style.size(); }
    
    
    
    
    /**Access to parse result.*/
    public Text_list_style get_text_list_style() { return text_list_style; }
    
    
  
  }




  /**Class for Component Office_body. */
  public static class Office_body_Base extends SrcInfo {
  
  
    
    protected Office_drawing office_drawing;
    
    
    
    
    /**Access to parse result.*/
    public Office_drawing get_office_drawing() { return office_drawing; }
    
    
  
  }




  /**Class for Component Office_font_face_decls. */
  public static class Office_font_face_decls_Base extends SrcInfo {
  
  
    
    protected List<Style_font_face> style_font_face;
    
    
    
    
    /**Access to parse result, get the elements of the container style_font_face*/
    public Iterable<Style_font_face> get_style_font_face() { return style_font_face; }
    
    /**Access to parse result, get the size of the container style_font_face.*/
    public int getSize_style_font_face() { return style_font_face ==null ? 0 : style_font_face.size(); }
    
    
  
  }




  /**Class for Component Style_style. */
  public static class Style_style_Base extends SrcInfo {
  
  
    
    protected String style_family;
    
    
    
    protected String style_name;
    
    
    
    protected String style_parent_style_name;
    
    
    
    protected Loext_graphic_properties loext_graphic_properties;
    
    
    
    protected Style_graphic_properties style_graphic_properties;
    
    
    
    protected Style_paragraph_properties style_paragraph_properties;
    
    
    
    protected Style_text_properties style_text_properties;
    
    
    
    
    /**Access to parse result.*/
    public String get_style_family() { return style_family; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_name() { return style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_parent_style_name() { return style_parent_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public Loext_graphic_properties get_loext_graphic_properties() { return loext_graphic_properties; }
    
    
    
    
    /**Access to parse result.*/
    public Style_graphic_properties get_style_graphic_properties() { return style_graphic_properties; }
    
    
    
    
    /**Access to parse result.*/
    public Style_paragraph_properties get_style_paragraph_properties() { return style_paragraph_properties; }
    
    
    
    
    /**Access to parse result.*/
    public Style_text_properties get_style_text_properties() { return style_text_properties; }
    
    
  
  }




  /**Class for Component Text_list_style. */
  public static class Text_list_style_Base extends SrcInfo {
  
  
    
    protected String style_name;
    
    
    
    protected List<Text_list_level_style_bullet> text_list_level_style_bullet;
    
    
    
    
    /**Access to parse result.*/
    public String get_style_name() { return style_name; }
    
    
    
    
    /**Access to parse result, get the elements of the container text_list_level_style_bullet*/
    public Iterable<Text_list_level_style_bullet> get_text_list_level_style_bullet() { return text_list_level_style_bullet; }
    
    /**Access to parse result, get the size of the container text_list_level_style_bullet.*/
    public int getSize_text_list_level_style_bullet() { return text_list_level_style_bullet ==null ? 0 : text_list_level_style_bullet.size(); }
    
    
  
  }




  /**Class for Component Office_drawing. */
  public static class Office_drawing_Base extends SrcInfo {
  
  
    
    protected Draw_page draw_page;
    
    
    
    
    /**Access to parse result.*/
    public Draw_page get_draw_page() { return draw_page; }
    
    
  
  }




  /**Class for Component Style_font_face. */
  public static class Style_font_face_Base extends SrcInfo {
  
  
    
    protected String style_font_adornments;
    
    
    
    protected String style_font_family_generic;
    
    
    
    protected String style_font_pitch;
    
    
    
    protected String style_name;
    
    
    
    protected String svg_font_family;
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_adornments() { return style_font_adornments; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_family_generic() { return style_font_family_generic; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_pitch() { return style_font_pitch; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_name() { return style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_font_family() { return svg_font_family; }
    
    
  
  }




  /**Class for Component Loext_graphic_properties. */
  public static class Loext_graphic_properties_Base extends SrcInfo {
  
  
    
    protected String draw_fill;
    
    
    
    protected String draw_fill_color;
    
    
    
    protected String draw_opacity;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_fill() { return draw_fill; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_fill_color() { return draw_fill_color; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_opacity() { return draw_opacity; }
    
    
  
  }




  /**Class for Component Style_graphic_properties. */
  public static class Style_graphic_properties_Base extends SrcInfo {
  
  
    
    protected String draw_auto_grow_height;
    
    
    
    protected String draw_auto_grow_width;
    
    
    
    protected String draw_fill;
    
    
    
    protected String draw_fill_color;
    
    
    
    protected String draw_marker_end;
    
    
    
    protected String draw_marker_end_width;
    
    
    
    protected String draw_opacity;
    
    
    
    protected String draw_shadow_opacity;
    
    
    
    protected String draw_stroke;
    
    
    
    protected String draw_textarea_horizontal_align;
    
    
    
    protected String draw_textarea_vertical_align;
    
    
    
    protected String fo_min_height;
    
    
    
    protected String fo_min_width;
    
    
    
    protected String svg_stroke_color;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_auto_grow_height() { return draw_auto_grow_height; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_auto_grow_width() { return draw_auto_grow_width; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_fill() { return draw_fill; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_fill_color() { return draw_fill_color; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_marker_end() { return draw_marker_end; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_marker_end_width() { return draw_marker_end_width; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_opacity() { return draw_opacity; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_shadow_opacity() { return draw_shadow_opacity; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_stroke() { return draw_stroke; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_textarea_horizontal_align() { return draw_textarea_horizontal_align; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_textarea_vertical_align() { return draw_textarea_vertical_align; }
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_min_height() { return fo_min_height; }
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_min_width() { return fo_min_width; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_stroke_color() { return svg_stroke_color; }
    
    
  
  }




  /**Class for Component Style_paragraph_properties. */
  public static class Style_paragraph_properties_Base extends SrcInfo {
  
  
    
    protected String fo_text_align;
    
    
    
    protected String style_text_autospace;
    
    
    
    protected String style_writing_mode;
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_text_align() { return fo_text_align; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_text_autospace() { return style_text_autospace; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_writing_mode() { return style_writing_mode; }
    
    
  
  }




  /**Class for Component Style_text_properties. */
  public static class Style_text_properties_Base extends SrcInfo {
  
  
    
    protected String fo_color;
    
    
    
    protected String fo_font_family;
    
    
    
    protected String fo_font_size;
    
    
    
    protected String fo_font_style;
    
    
    
    protected String fo_font_variant;
    
    
    
    protected String fo_font_weight;
    
    
    
    protected String fo_text_shadow;
    
    
    
    protected String fo_text_transform;
    
    
    
    protected String style_font_name_asian;
    
    
    
    protected String style_font_name_complex;
    
    
    
    protected String style_font_relief;
    
    
    
    protected String style_font_size_asian;
    
    
    
    protected String style_font_size_complex;
    
    
    
    protected String style_font_style_asian;
    
    
    
    protected String style_font_style_complex;
    
    
    
    protected String style_font_weight_asian;
    
    
    
    protected String style_font_weight_complex;
    
    
    
    protected String style_letter_kerning;
    
    
    
    protected String style_text_emphasize;
    
    
    
    protected String style_text_line_through_style;
    
    
    
    protected String style_text_line_through_type;
    
    
    
    protected String style_text_outline;
    
    
    
    protected String style_text_overline_color;
    
    
    
    protected String style_text_overline_style;
    
    
    
    protected String style_text_underline_style;
    
    
    
    protected String style_use_window_font_color;
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_color() { return fo_color; }
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_font_family() { return fo_font_family; }
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_font_size() { return fo_font_size; }
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_font_style() { return fo_font_style; }
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_font_variant() { return fo_font_variant; }
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_font_weight() { return fo_font_weight; }
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_text_shadow() { return fo_text_shadow; }
    
    
    
    
    /**Access to parse result.*/
    public String get_fo_text_transform() { return fo_text_transform; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_name_asian() { return style_font_name_asian; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_name_complex() { return style_font_name_complex; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_relief() { return style_font_relief; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_size_asian() { return style_font_size_asian; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_size_complex() { return style_font_size_complex; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_style_asian() { return style_font_style_asian; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_style_complex() { return style_font_style_complex; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_weight_asian() { return style_font_weight_asian; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_font_weight_complex() { return style_font_weight_complex; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_letter_kerning() { return style_letter_kerning; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_text_emphasize() { return style_text_emphasize; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_text_line_through_style() { return style_text_line_through_style; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_text_line_through_type() { return style_text_line_through_type; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_text_outline() { return style_text_outline; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_text_overline_color() { return style_text_overline_color; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_text_overline_style() { return style_text_overline_style; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_text_underline_style() { return style_text_underline_style; }
    
    
    
    
    /**Access to parse result.*/
    public String get_style_use_window_font_color() { return style_use_window_font_color; }
    
    
  
  }




  /**Class for Component Text_list_level_style_bullet. */
  public static class Text_list_level_style_bullet_Base extends SrcInfo {
  
  
    
    protected String text_bullet_char;
    
    
    
    protected String text_level;
    
    
    
    protected Style_list_level_properties style_list_level_properties;
    
    
    
    protected Style_text_properties style_text_properties;
    
    
    
    
    /**Access to parse result.*/
    public String get_text_bullet_char() { return text_bullet_char; }
    
    
    
    
    /**Access to parse result.*/
    public String get_text_level() { return text_level; }
    
    
    
    
    /**Access to parse result.*/
    public Style_list_level_properties get_style_list_level_properties() { return style_list_level_properties; }
    
    
    
    
    /**Access to parse result.*/
    public Style_text_properties get_style_text_properties() { return style_text_properties; }
    
    
  
  }




  /**Class for Component Draw_page. */
  public static class Draw_page_Base extends SrcInfo {
  
  
    
    protected String draw_master_page_name;
    
    
    
    protected String draw_name;
    
    
    
    protected String draw_style_name;
    
    
    
    protected List<Draw_connector> draw_connector;
    
    
    
    protected List<Draw_custom_shape> draw_custom_shape;
    
    
    
    protected List<Draw_frame> draw_frame;
    
    
    
    protected List<Draw_g> draw_g;
    
    
    
    protected List<Draw_polygon> draw_polygon;
    
    
    
    protected Draw_polyline draw_polyline;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_master_page_name() { return draw_master_page_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_name() { return draw_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_style_name() { return draw_style_name; }
    
    
    
    
    /**Access to parse result, get the elements of the container draw_connector*/
    public Iterable<Draw_connector> get_draw_connector() { return draw_connector; }
    
    /**Access to parse result, get the size of the container draw_connector.*/
    public int getSize_draw_connector() { return draw_connector ==null ? 0 : draw_connector.size(); }
    
    
    
    
    /**Access to parse result, get the elements of the container draw_custom_shape*/
    public Iterable<Draw_custom_shape> get_draw_custom_shape() { return draw_custom_shape; }
    
    /**Access to parse result, get the size of the container draw_custom_shape.*/
    public int getSize_draw_custom_shape() { return draw_custom_shape ==null ? 0 : draw_custom_shape.size(); }
    
    
    
    
    /**Access to parse result, get the elements of the container draw_frame*/
    public Iterable<Draw_frame> get_draw_frame() { return draw_frame; }
    
    /**Access to parse result, get the size of the container draw_frame.*/
    public int getSize_draw_frame() { return draw_frame ==null ? 0 : draw_frame.size(); }
    
    
    
    
    /**Access to parse result.*/
    public Iterable<Draw_g> get_draw_g() { return draw_g; }
    
    
    
    
    /**Access to parse result, get the elements of the container draw_polygon*/
    public Iterable<Draw_polygon> get_draw_polygon() { return draw_polygon; }
    
    /**Access to parse result, get the size of the container draw_polygon.*/
    public int getSize_draw_polygon() { return draw_polygon ==null ? 0 : draw_polygon.size(); }
    
    
    
    
    /**Access to parse result.*/
    public Draw_polyline get_draw_polyline() { return draw_polyline; }
    
    
  
  }




  /**Class for Component Style_list_level_properties. */
  public static class Style_list_level_properties_Base extends SrcInfo {
  
  
    
    protected String text_min_label_width;
    
    
    
    protected String text_space_before;
    
    
    
    
    /**Access to parse result.*/
    public String get_text_min_label_width() { return text_min_label_width; }
    
    
    
    
    /**Access to parse result.*/
    public String get_text_space_before() { return text_space_before; }
    
    
  
  }




  /**Class for Component Draw_connector. */
  public static class Draw_connector_Base extends SrcInfo {
  
  
    
    protected String draw_end_glue_point;
    
    
    
    protected String draw_end_shape;
    
    
    
    protected String draw_layer;
    
    
    
    protected String draw_line_skew;
    
    
    
    protected String draw_start_glue_point;
    
    
    
    protected String draw_start_shape;
    
    
    
    protected String draw_style_name;
    
    
    
    protected String draw_text_style_name;
    
    
    
    protected String svg_d;
    
    
    
    protected String svg_viewBox;
    
    
    
    protected String svg_x1;
    
    
    
    protected String svg_x2;
    
    
    
    protected String svg_y1;
    
    
    
    protected String svg_y2;
    
    
    
    protected String text_p;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_end_glue_point() { return draw_end_glue_point; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_end_shape() { return draw_end_shape; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_layer() { return draw_layer; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_line_skew() { return draw_line_skew; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_start_glue_point() { return draw_start_glue_point; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_start_shape() { return draw_start_shape; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_style_name() { return draw_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_text_style_name() { return draw_text_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_d() { return svg_d; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_viewBox() { return svg_viewBox; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_x1() { return svg_x1; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_x2() { return svg_x2; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_y1() { return svg_y1; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_y2() { return svg_y2; }
    
    
    
    
    /**Access to parse result.*/
    public String get_text_p() { return text_p; }
    
    
  
  }




  /**Class for Component Draw_custom_shape. */
  public static class Draw_custom_shape_Base extends SrcInfo {
  
  
    
    protected String draw_id;
    
    
    
    protected String draw_layer;
    
    
    
    protected String draw_style_name;
    
    
    
    protected String draw_text_style_name;
    
    
    
    protected String svg_height;
    
    
    
    protected String svg_width;
    
    
    
    protected String svg_x;
    
    
    
    protected String svg_y;
    
    
    
    protected String xml_id;
    
    
    
    protected Draw_enhanced_geometry draw_enhanced_geometry;
    
    
    
    protected Text_p text_p;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_id() { return draw_id; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_layer() { return draw_layer; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_style_name() { return draw_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_text_style_name() { return draw_text_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_height() { return svg_height; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_width() { return svg_width; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_x() { return svg_x; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_y() { return svg_y; }
    
    
    
    
    /**Access to parse result.*/
    public String get_xml_id() { return xml_id; }
    
    
    
    
    /**Access to parse result.*/
    public Draw_enhanced_geometry get_draw_enhanced_geometry() { return draw_enhanced_geometry; }
    
    
    
    
    /**Access to parse result.*/
    public Text_p get_text_p() { return text_p; }
    
    
  
  }




  /**Class for Component Draw_frame. */
  public static class Draw_frame_Base extends SrcInfo {
  
  
    
    protected String draw_id;
    
    
    
    protected String draw_layer;
    
    
    
    protected String draw_style_name;
    
    
    
    protected String draw_text_style_name;
    
    
    
    protected String svg_height;
    
    
    
    protected String svg_width;
    
    
    
    protected String svg_x;
    
    
    
    protected String svg_y;
    
    
    
    protected String xml_id;
    
    
    
    protected Draw_text_box draw_text_box;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_id() { return draw_id; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_layer() { return draw_layer; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_style_name() { return draw_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_text_style_name() { return draw_text_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_height() { return svg_height; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_width() { return svg_width; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_x() { return svg_x; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_y() { return svg_y; }
    
    
    
    
    /**Access to parse result.*/
    public String get_xml_id() { return xml_id; }
    
    
    
    
    /**Access to parse result.*/
    public Draw_text_box get_draw_text_box() { return draw_text_box; }
    
    
  
  }




  /**Class for Component Draw_g. */
  public static class Draw_g_Base extends SrcInfo {
  
  
    
    protected String draw_id;
    
    
    
    protected String xml_id;
    
    
    protected List<Draw_frame> draw_frame;
    
    
    
    protected Draw_polygon draw_polygon;
    
    
    
    
    /**Access to parse result, get the elements of the container draw_frame*/
    public Iterable<Draw_frame> get_draw_frame() { return draw_frame; }
    
    /**Access to parse result, get the size of the container draw_frame.*/
    public int getSize_draw_frame() { return draw_frame ==null ? 0 : draw_frame.size(); }
    
    
    
    
    /**Access to parse result.*/
    public Draw_polygon get_draw_polygon() { return draw_polygon; }
    
    

    
    
    /**Access to parse result.*/
    public String get_draw_id() { return draw_id; }
    
    
    
    
    /**Access to parse result.*/
    public String get_xml_id() { return xml_id; }
    
    
    
    
    
  
  }




  /**Class for Component Draw_polygon. */
  public static class Draw_polygon_Base extends SrcInfo {
  
  
    
    protected String draw_layer;
    
    
    
    protected String draw_points;
    
    
    
    protected String draw_style_name;
    
    
    
    protected String draw_text_style_name;
    
    
    
    protected String svg_height;
    
    
    
    protected String svg_viewBox;
    
    
    
    protected String svg_width;
    
    
    
    protected String svg_x;
    
    
    
    protected String svg_y;
    
    
    
    protected String text_p;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_layer() { return draw_layer; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_points() { return draw_points; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_style_name() { return draw_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_text_style_name() { return draw_text_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_height() { return svg_height; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_viewBox() { return svg_viewBox; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_width() { return svg_width; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_x() { return svg_x; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_y() { return svg_y; }
    
    
    
    
    /**Access to parse result.*/
    public String get_text_p() { return text_p; }
    
    
  
  }




  /**Class for Component Draw_polyline. */
  public static class Draw_polyline_Base extends SrcInfo {
  
  
    
    protected String draw_layer;
    
    
    
    protected String draw_points;
    
    
    
    protected String draw_style_name;
    
    
    
    protected String draw_text_style_name;
    
    
    
    protected String draw_transform;
    
    
    
    protected String svg_height;
    
    
    
    protected String svg_viewBox;
    
    
    
    protected String svg_width;
    
    
    
    protected String text_p;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_layer() { return draw_layer; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_points() { return draw_points; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_style_name() { return draw_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_text_style_name() { return draw_text_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_transform() { return draw_transform; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_height() { return svg_height; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_viewBox() { return svg_viewBox; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_width() { return svg_width; }
    
    
    
    
    /**Access to parse result.*/
    public String get_text_p() { return text_p; }
    
    
  
  }




  /**Class for Component Draw_enhanced_geometry. */
  public static class Draw_enhanced_geometry_Base extends SrcInfo {
  
  
    
    protected String draw_enhanced_path;
    
    
    
    protected String draw_glue_points;
    
    
    
    protected String draw_mirror_horizontal;
    
    
    
    protected String draw_mirror_vertical;
    
    
    
    protected String draw_modifiers;
    
    
    
    protected String draw_text_areas;
    
    
    
    protected String draw_type;
    
    
    
    protected String svg_viewBox;
    
    
    
    protected List<Draw_equation> draw_equation;
    
    
    
    protected Draw_handle draw_handle;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_enhanced_path() { return draw_enhanced_path; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_glue_points() { return draw_glue_points; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_mirror_horizontal() { return draw_mirror_horizontal; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_mirror_vertical() { return draw_mirror_vertical; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_modifiers() { return draw_modifiers; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_text_areas() { return draw_text_areas; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_type() { return draw_type; }
    
    
    
    
    /**Access to parse result.*/
    public String get_svg_viewBox() { return svg_viewBox; }
    
    
    
    
    /**Access to parse result, get the elements of the container draw_equation*/
    public Iterable<Draw_equation> get_draw_equation() { return draw_equation; }
    
    /**Access to parse result, get the size of the container draw_equation.*/
    public int getSize_draw_equation() { return draw_equation ==null ? 0 : draw_equation.size(); }
    
    
    
    
    /**Access to parse result.*/
    public Draw_handle get_draw_handle() { return draw_handle; }
    
    
  
  }




  /**Class for Component Text_p. */
  public static class Text_p_Base extends SrcInfo {
  
  
    
    protected String text_style_name;
    
    
    
    protected String text_s;
    
    
    
    protected Text_span text_span;
    
    
    
    
    /**Access to parse result.*/
    public String get_text_style_name() { return text_style_name; }
    
    
    
    
    /**Access to parse result.*/
    public String get_text_s() { return text_s; }
    
    
    
    
    /**Access to parse result.*/
    public Text_span get_text_span() { return text_span; }
    
    
  
  }




  /**Class for Component Draw_text_box. */
  public static class Draw_text_box_Base extends SrcInfo {
  
  
    
    protected Text_p text_p;
    
    
    
    
    /**Access to parse result.*/
    public Text_p get_text_p() { return text_p; }
    
    
  
  }








  /**Class for Component Draw_equation. */
  public static class Draw_equation_Base extends SrcInfo {
  
  
    
    protected String draw_formula;
    
    
    
    protected String draw_name;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_formula() { return draw_formula; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_name() { return draw_name; }
    
    
  
  }




  /**Class for Component Draw_handle. */
  public static class Draw_handle_Base extends SrcInfo {
  
  
    
    protected String draw_handle_position;
    
    
    
    protected String draw_handle_range_x_maximum;
    
    
    
    protected String draw_handle_range_x_minimum;
    
    
    
    protected String draw_handle_range_y_maximum;
    
    
    
    protected String draw_handle_range_y_minimum;
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_handle_position() { return draw_handle_position; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_handle_range_x_maximum() { return draw_handle_range_x_maximum; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_handle_range_x_minimum() { return draw_handle_range_x_minimum; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_handle_range_y_maximum() { return draw_handle_range_y_maximum; }
    
    
    
    
    /**Access to parse result.*/
    public String get_draw_handle_range_y_minimum() { return draw_handle_range_y_minimum; }
    
    
  
  }




  /**Class for Component Text_span. */
  public static class Text_span_Base extends SrcInfo {
  
  
    
    protected String text_style_name;
    
    
    protected String text;
    
    /**Access to parse result.*/
    public String get_text_style_name() { return text_style_name; }
    
    
    /**Access to parse result.*/
    public String get_text() { return text; }
    
    
  
  }


}

