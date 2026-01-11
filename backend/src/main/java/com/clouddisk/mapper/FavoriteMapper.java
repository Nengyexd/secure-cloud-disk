package com.clouddisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clouddisk.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏Mapper
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
