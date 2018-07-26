package com.westlake.air.pecs.domain.query;

import org.springframework.data.domain.Sort;

import java.io.Serializable;

/**
 * @author fengzhi
 * Date 2017-01-03
 */
public class PageQuery implements Serializable {

    private static final long serialVersionUID = -8745138167696978267L;

    public static final int DEFAULT_PAGE_SIZE = 40;
    public static final String DEFAULT_SORT_COLUMN = "createDate";

    protected int pageNo = 1;
    protected int pageSize = DEFAULT_PAGE_SIZE;
    protected int start = 0;
    protected Sort.Direction orderBy = Sort.Direction.DESC;
    protected String sortColumn = DEFAULT_SORT_COLUMN;
    protected long totalNum = 0;

    protected int totalPage = 0 ;

    protected PageQuery() {}

    public PageQuery(int pageNo, int pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(final int pageNo) {
        this.pageNo = pageNo;

        if (pageNo < 1) {
            this.pageNo = 1;
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;

        if (pageSize < 1) {
            this.pageSize = DEFAULT_PAGE_SIZE;
        }
    }

    public Integer getFirst() {
        return (getPageNo() > 0 && getPageSize() > 0) ? ((getPageNo() - 1) * getPageSize() + 0) : 0;
        /*
         * maysql=Integer((pageNo - 1) * pageSize + 0); oralce=Integer((pageNo -
		 * 1) * pageSize + 1);
		 */
    }

    public int getRowStart() {
        return getFirst();
    }

    public Integer getLast() {
        return (getFirst() + getPageSize() - 1);
    }

    public long getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(long totalNum) {
        this.totalNum = totalNum;
    }

    public int getStart() {
        this.start = (this.pageNo - 1) * this.pageSize;
        return start;
    }

    public long  getTotalPage() {
        if(this.pageSize> 0 && this.totalNum >0)
        {
            return ( this.totalNum % this.pageSize == 0 ? (this.totalNum / this.pageSize) : ( this.totalNum / this.pageSize +1 )) ;

        }else
            return 0 ;
    }

    public Sort.Direction getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(Sort.Direction orderBy) {
        this.orderBy = orderBy;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

}