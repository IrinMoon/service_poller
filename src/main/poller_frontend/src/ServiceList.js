import React from 'react';
import './ServiceList.css'

class ServiceList extends React.Component {
  constructor(props) {
    super(props);
    this.toService = this.toService.bind(this);
  }

  toService(s) {
    return (
      <tbody key={s.id}>
        <tr>
          <td>{s.name}</td>
          <td>{s.url}</td>
          <td>{s.created_at}</td>
          <td>{s.status}</td>
        </tr>
      </tbody>)
  }

  render() {
    return (
      <table className="service-list">
        <tbody>
          <tr>
            <th>Name</th>
            <th>URL</th>
            <th>Created At</th>
            <th>Status</th>
          </tr>
        </tbody>
        {this.props.services.map(this.toService)}
      </table>
    )
  }
}

export default ServiceList;

