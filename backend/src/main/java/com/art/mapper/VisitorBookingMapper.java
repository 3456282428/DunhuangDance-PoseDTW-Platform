package com.art.mapper;

import com.art.entity.VisitorBooking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface VisitorBookingMapper {
    VisitorBooking selectById(@Param("bookingId") Integer bookingId);
    int insert(VisitorBooking booking);
    int updateStatus(@Param("bookingId") Integer bookingId,
                     @Param("status") String status,
                     @Param("handleTime") LocalDateTime handleTime);
    List<VisitorBooking> selectByPhone(@Param("phone") String phone);
    List<VisitorBooking> selectByCondition(@Param("status") String status,
                                           @Param("keyword") String keyword);
}
